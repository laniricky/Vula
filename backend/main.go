package main

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"log"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/gofiber/contrib/websocket"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/cors"
	"github.com/google/uuid"
	"github.com/gofiber/fiber/v2/middleware/logger"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/redis/go-redis/v9"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

var (
	rdb    *redis.Client
	db     *gorm.DB
	minioC *minio.Client
	hub    *Hub
	ctx    = context.Background()
)

// DEV_OTP_BYPASS — when set to a non-empty string, any code equal to this
// value is accepted without Redis verification. Disabled in production.
const DEV_OTP_BYPASS = "123456"

func initRedis() {
	rdb = redis.NewClient(&redis.Options{Addr: "redis:6379"})
	if _, err := rdb.Ping(ctx).Result(); err != nil {
		log.Println("Redis not connected. OTP simulated.")
		rdb = nil
	} else {
		log.Println("Connected to Redis")
	}
}

func initDB() {
	dsn := os.Getenv("DB_URL")
	if dsn == "" {
		dsn = "postgres://vula_admin:secure_password@postgres:5432/vula?sslmode=disable"
	}
	var err error
	db, err = gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatal("Failed to connect to PostgreSQL:", err)
	}
	err = db.AutoMigrate(&User{}, &Post{}, &Reaction{}, &ChatRoom{}, &ChatMessage{},
		&Follow{}, &Story{}, &MessageRequest{})
	if err != nil {
		log.Fatal("Failed to migrate database:", err)
	}
	log.Println("Connected to PostgreSQL and migrated schema")
}

func initMinio() {
	endpoint  := os.Getenv("MINIO_ENDPOINT")
	if endpoint == "" { endpoint = "minio:9000" }
	accessKey := os.Getenv("MINIO_ROOT_USER")
	secretKey  := os.Getenv("MINIO_ROOT_PASSWORD")
	if accessKey == "" { accessKey = "vula_admin"; secretKey = "secure_password" }

	var err error
	minioC, err = minio.New(endpoint, &minio.Options{
		Creds: credentials.NewStaticV4(accessKey, secretKey, ""), Secure: false,
	})
	if err != nil {
		log.Println("Failed to initialize MinIO Client:", err)
		return
	}
	log.Println("Connected to MinIO")
	for _, b := range []string{"profiles", "posts", "stories", "chat-media", "voice"} {
		exists, _ := minioC.BucketExists(ctx, b)
		if !exists {
			minioC.MakeBucket(ctx, b, minio.MakeBucketOptions{})
			// Make bucket public-readable
			policy := `{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::` + b + `/*"]}]}`
			minioC.SetBucketPolicy(ctx, b, policy)
		}
	}
}

// saveToMinio uploads bytes to a MinIO bucket and returns the public URL.
func saveToMinio(bucket, objectName, contentType string, data []byte) (string, error) {
	if minioC == nil { return "", fmt.Errorf("minio not configured") }
	_, err := minioC.PutObject(ctx, bucket, objectName, bytes.NewReader(data), int64(len(data)),
		minio.PutObjectOptions{ContentType: contentType})
	if err != nil { return "", err }
	endpoint := os.Getenv("MINIO_ENDPOINT")
	if endpoint == "" { endpoint = "minio:9000" }
	publicHost := os.Getenv("MINIO_PUBLIC_HOST")
	if publicHost == "" { publicHost = "http://" + endpoint }
	return publicHost + "/" + bucket + "/" + objectName, nil
}

// ── JWT helpers (mock — replace with real JWT in production) ──────────────────

func makeToken(userID string) string  { return "mock-jwt-for-" + userID }
func userIDFromToken(token string) string {
	return strings.TrimPrefix(token, "mock-jwt-for-")
}

func authMiddleware(c *fiber.Ctx) error {
	auth := c.Get("Authorization")
	if !strings.HasPrefix(auth, "Bearer ") {
		return c.Status(401).JSON(fiber.Map{"error": "Unauthorized"})
	}
	uid := userIDFromToken(strings.TrimPrefix(auth, "Bearer "))
	if uid == "" {
		return c.Status(401).JSON(fiber.Map{"error": "Unauthorized"})
	}
	c.Locals("uid", uid)
	return c.Next()
}

func currentUID(c *fiber.Ctx) string { return c.Locals("uid").(string) }

// ── Twilio SMS ────────────────────────────────────────────────────────────────

func sendTwilioSMS(to, code string) {
	accountSid := os.Getenv("TWILIO_ACCOUNT_SID")
	authToken  := os.Getenv("TWILIO_AUTH_TOKEN")
	fromPhone  := os.Getenv("TWILIO_PHONE_NUMBER")

	if accountSid == "" || authToken == "" || fromPhone == "" {
		log.Printf("===> SMS OTP for %s: %s <===", to, code)
		return
	}
	urlStr  := fmt.Sprintf("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid)
	msgData := url.Values{}
	msgData.Set("To", to)
	msgData.Set("From", fromPhone)
	msgData.Set("Body", fmt.Sprintf("Your Vula code is %s", code))
	req, err := http.NewRequest("POST", urlStr, strings.NewReader(msgData.Encode()))
	if err != nil { return }
	req.SetBasicAuth(accountSid, authToken)
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")
	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil { return }
	defer resp.Body.Close()
}

// ─────────────────────────────────────────────────────────────────────────────
// main
// ─────────────────────────────────────────────────────────────────────────────

func main() {
	initRedis()
	initDB()
	initMinio()

	hub = newHub()
	go hub.run()

	app := fiber.New(fiber.Config{BodyLimit: 50 * 1024 * 1024}) // 50 MB uploads
	app.Use(logger.New())
	app.Use(cors.New(cors.Config{AllowOrigins: "*", AllowHeaders: "Authorization,Content-Type"}))

	// ── Health ────────────────────────────────────────────────────────────────
	app.Get("/health", func(c *fiber.Ctx) error { return c.SendString("Vula Backend OK") })

	// ── AUTH ──────────────────────────────────────────────────────────────────
	auth := app.Group("/api/auth")

	auth.Post("/request-code", func(c *fiber.Ctx) error {
		var req struct{ Phone string `json:"phone"` }
		if err := c.BodyParser(&req); err != nil {
			return c.Status(400).JSON(fiber.Map{"error": "Invalid request"})
		}
		n, _ := rand.Int(rand.Reader, big.NewInt(900000))
		code := fmt.Sprintf("%06d", n.Int64()+100000)
		if rdb != nil { rdb.Set(ctx, "otp:"+req.Phone, code, 5*time.Minute) }
		go sendTwilioSMS(req.Phone, code)
		return c.JSON(fiber.Map{"message": "Code sent"})
	})

	auth.Post("/verify-code", func(c *fiber.Ctx) error {
		var req struct {
			Phone string `json:"phone"`
			Code  string `json:"code"`
		}
		if err := c.BodyParser(&req); err != nil {
			return c.Status(400).JSON(fiber.Map{"error": "Invalid request"})
		}

		// Dev bypass
		bypassAllowed := req.Code == DEV_OTP_BYPASS && os.Getenv("GO_ENV") != "production"

		if !bypassAllowed && rdb != nil {
			val, err := rdb.Get(ctx, "otp:"+req.Phone).Result()
			if err != nil || val != req.Code {
				return c.Status(400).JSON(fiber.Map{"error": "Invalid code"})
			}
			rdb.Del(ctx, "otp:"+req.Phone)
		}

		var user User
		result := db.Where("phone_number = ?", req.Phone).First(&user)
		if result.Error != nil {
			newID := uuid.New().String()
			user = User{ID: newID, PhoneNumber: req.Phone}
			if err := db.Create(&user).Error; err != nil {
				log.Printf("Failed to create user: %v", err)
				return c.Status(500).JSON(fiber.Map{"error": "Failed to create user"})
			}
			// Re-fetch to ensure all fields are populated
			db.Where("id = ?", newID).First(&user)
		}

		return c.JSON(fiber.Map{
			"message":  "Authentication successful",
			"token":    makeToken(user.ID),
			"userId":   user.ID,
			"username": user.Username,
		})
	})

	// ── All protected routes require auth ─────────────────────────────────────
	api := app.Group("/api", authMiddleware)

	// ── USERS ─────────────────────────────────────────────────────────────────
	api.Get("/users/me", func(c *fiber.Ctx) error {
		var u User
		db.First(&u, "id = ?", currentUID(c))
		return c.JSON(userToMap(u))
	})

	api.Put("/users/me", func(c *fiber.Ctx) error {
		var req struct {
			DisplayName     string `json:"displayName"`
			Username        string `json:"username"`
			Bio             string `json:"bio"`
			RichStatus      string `json:"richStatus"`
			Website         string `json:"website"`
			IsPrivate       bool   `json:"isPrivate"`
			ProfileImageUrl string `json:"profileImageUrl"`
			BannerUrl       string `json:"bannerUrl"`
		}
		c.BodyParser(&req)
		uid := currentUID(c)
		
		updates := map[string]interface{}{
			"username":     req.Username,
			"bio":          req.Bio,
			"rich_status":  req.RichStatus,
			"display_name": req.DisplayName,
		}
		if req.ProfileImageUrl != "" {
			updates["profile_image_url"] = req.ProfileImageUrl
		}
		if req.BannerUrl != "" {
			updates["banner_url"] = req.BannerUrl
		}
		
		db.Model(&User{}).Where("id = ?", uid).Updates(updates)
		var u User; db.First(&u, "id = ?", uid)
		return c.JSON(userToMap(u))
	})

	api.Get("/users/:userId", func(c *fiber.Ctx) error {
		var u User
		if err := db.First(&u, "id = ?", c.Params("userId")).Error; err != nil {
			return c.Status(404).JSON(fiber.Map{"error": "User not found"})
		}
		return c.JSON(userToMap(u))
	})

	api.Get("/users/search", func(c *fiber.Ctx) error {
		q     := "%" + strings.ToLower(c.Query("q")) + "%"
		limit := 25
		var users []User
		db.Where("LOWER(username) LIKE ? OR LOWER(phone_number) LIKE ?", q, q).Limit(limit).Find(&users)
		out := make([]map[string]interface{}, len(users))
		for i, u := range users { out[i] = userToMap(u) }
		return c.JSON(out)
	})

	api.Get("/users/suggested", func(c *fiber.Ctx) error {
		var users []User
		db.Where("id != ?", currentUID(c)).Order("created_at DESC").Limit(15).Find(&users)
		out := make([]map[string]interface{}, len(users))
		for i, u := range users { out[i] = userToMap(u) }
		return c.JSON(out)
	})

	// ── FOLLOW ────────────────────────────────────────────────────────────────
	api.Post("/users/:userId/follow", func(c *fiber.Ctx) error {
		f := Follow{FollowerID: currentUID(c), FollowedID: c.Params("userId")}
		db.Where(f).FirstOrCreate(&f)
		return c.SendStatus(200)
	})

	api.Delete("/users/:userId/follow", func(c *fiber.Ctx) error {
		db.Where("follower_id = ? AND followed_id = ?", currentUID(c), c.Params("userId")).Delete(&Follow{})
		return c.SendStatus(200)
	})

	api.Get("/users/:userId/follow-status", func(c *fiber.Ctx) error {
		var count int64
		db.Model(&Follow{}).Where("follower_id = ? AND followed_id = ?", currentUID(c), c.Params("userId")).Count(&count)
		return c.JSON(fiber.Map{"isFollowing": count > 0})
	})

	// ── POSTS ─────────────────────────────────────────────────────────────────
	api.Get("/posts/feed", func(c *fiber.Ctx) error {
		page  := c.QueryInt("page", 1)
		limit := c.QueryInt("limit", 20)
		offset := (page - 1) * limit
		var posts []Post
		db.Preload("Author").Order("created_at DESC").Limit(limit).Offset(offset).Find(&posts)
		return c.JSON(postsToSlice(posts))
	})

	api.Get("/posts/user/:userId", func(c *fiber.Ctx) error {
		var posts []Post
		db.Preload("Author").Where("author_id = ?", c.Params("userId")).
			Order("created_at DESC").Find(&posts)
		return c.JSON(postsToSlice(posts))
	})

	api.Get("/posts/explore", func(c *fiber.Ctx) error {
		filter := c.Query("filter", "trending")
		limit  := c.QueryInt("limit", 40)
		var posts []Post
		q := db.Preload("Author").Limit(limit)
		switch filter {
		case "clips":
			q = q.Where("video_url != ''").Order("created_at DESC")
		case "new":
			q = q.Order("created_at DESC")
		default: // trending
			q = q.Order("created_at DESC")
		}
		q.Find(&posts)
		return c.JSON(postsToSlice(posts))
	})

	// Post creation: handles both JSON (seed) and multipart (app camera upload)
	api.Post("/posts", func(c *fiber.Ctx) error {
		uid := currentUID(c)
		var author User; db.First(&author, "id = ?", uid)

		var caption, mediaType, imageURL, videoURL string

		contentType := c.Get("Content-Type")
		if strings.Contains(contentType, "multipart/form-data") {
			// ── Multipart upload from app camera ──────────────────────────────
			caption   = c.FormValue("caption")
			mediaType = c.FormValue("mediaType")
			if mediaType == "" { mediaType = "image" }

			file, err := c.FormFile("media")
			if err == nil && file != nil {
				src, err2 := file.Open()
				if err2 == nil {
					defer src.Close()
					data := make([]byte, file.Size)
					src.Read(data)
					ext := "jpg"
					ct  := "image/jpeg"
					if mediaType == "video" { ext = "mp4"; ct = "video/mp4" }
					objName := uuid.New().String() + "." + ext
					pubURL, minioErr := saveToMinio("posts", objName, ct, data)
					if minioErr == nil {
						if mediaType == "video" { videoURL = pubURL } else { imageURL = pubURL }
					}
				}
			}
		} else {
			// ── JSON from seed script ─────────────────────────────────────────
			var req struct {
				Caption   string `json:"caption"`
				MediaType string `json:"mediaType"`
				ImageURL  string `json:"imageUrl"`
				VideoURL  string `json:"videoUrl"`
			}
			c.BodyParser(&req)
			caption   = req.Caption
			mediaType = req.MediaType
			imageURL  = req.ImageURL
			videoURL  = req.VideoURL
		}

		post := Post{
			ID:        uuid.New().String(),
			AuthorID:  uid,
			Author:    author,
			Caption:   caption,
			ImageURL:  imageURL,
			VideoURL:  videoURL,
			IsStory:   false,
			CreatedAt: time.Now(),
		}
		db.Create(&post)
		return c.Status(201).JSON(postToMap(post, caption, mediaType))
	})

	api.Post("/posts/:postId/react", func(c *fiber.Ctx) error {
		var req struct{ Emoji string `json:"emoji"` }
		c.BodyParser(&req)
		r := Reaction{PostID: c.Params("postId"), UserID: currentUID(c), Emoji: req.Emoji, CreatedAt: time.Now()}
		db.Where(Reaction{PostID: r.PostID, UserID: r.UserID}).Assign(r).FirstOrCreate(&r)
		return c.SendStatus(200)
	})

	api.Delete("/posts/:postId/react", func(c *fiber.Ctx) error {
		db.Where("post_id = ? AND user_id = ?", c.Params("postId"), currentUID(c)).Delete(&Reaction{})
		return c.SendStatus(200)
	})

	api.Post("/posts/:postId/comments", func(c *fiber.Ctx) error {
		return c.Status(201).JSON(fiber.Map{"id": "comment-1", "text": "ok"})
	})

	api.Get("/posts/:postId/comments", func(c *fiber.Ctx) error {
		return c.JSON([]interface{}{})
	})

	// ── STORIES ───────────────────────────────────────────────────────────────
	api.Get("/stories", func(c *fiber.Ctx) error {
		var stories []Story
		db.Preload("Author").Where("expires_at > ?", time.Now()).Find(&stories)
		out := make([]map[string]interface{}, 0)
		for _, s := range stories {
			out = append(out, fiber.Map{
				"id": s.ID, "authorId": s.AuthorID,
				"authorUsername": s.Author.Username,
				"authorProfileImageUrl": s.Author.ProfileImageURL,
				"imageUrl": s.ImageURL, "mediaType": "image",
				"createdAt": s.CreatedAt.UnixMilli(),
				"expiresAt": s.ExpiresAt.UnixMilli(),
			})
		}
		return c.JSON(out)
	})

	api.Post("/stories", func(c *fiber.Ctx) error {
		var req struct {
			ImageURL  string `json:"imageUrl"`
			MediaType string `json:"mediaType"`
		}
		c.BodyParser(&req)
		uid := currentUID(c)
		var author User; db.First(&author, "id = ?", uid)
		now := time.Now()
		s := Story{
			ID:        uuid.New().String(),
			AuthorID:  uid,
			Author:    author,
			ImageURL:  req.ImageURL,
			CreatedAt: now,
			ExpiresAt: now.Add(24 * time.Hour),
		}
		db.Create(&s)
		return c.Status(201).JSON(fiber.Map{
			"id": s.ID, "authorId": s.AuthorID,
			"authorUsername": s.Author.Username,
			"imageUrl": s.ImageURL, "mediaType": "image",
			"createdAt": s.CreatedAt.UnixMilli(),
			"expiresAt": s.ExpiresAt.UnixMilli(),
		})
	})

	// ── CHAT ──────────────────────────────────────────────────────────────────
	api.Get("/chat/rooms", func(c *fiber.Ctx) error {
		uid := currentUID(c)
		var rooms []ChatRoom
		db.Preload("Participants").Find(&rooms)
		var out []map[string]interface{}
		for _, r := range rooms {
			for _, p := range r.Participants {
				if p.ID == uid {
					out = append(out, chatRoomToMap(r))
					break
				}
			}
		}
		if out == nil { out = []map[string]interface{}{} }
		return c.JSON(out)
	})

	api.Post("/chat/rooms/direct", func(c *fiber.Ctx) error {
		var req struct{ OtherUserId string `json:"otherUserId"` }
		c.BodyParser(&req)
		uid := currentUID(c)
		if req.OtherUserId == "" {
			return c.Status(400).JSON(fiber.Map{"error": "otherUserId required"})
		}

		// Check for existing direct room
		var rooms []ChatRoom
		db.Preload("Participants").Find(&rooms)
		for _, r := range rooms {
			if !r.IsGroup && len(r.Participants) == 2 {
				hasMe, hasOther := false, false
				for _, p := range r.Participants {
					if p.ID == uid { hasMe = true }
					if p.ID == req.OtherUserId { hasOther = true }
				}
				if hasMe && hasOther { return c.JSON(fiber.Map{"roomId": r.ID}) }
			}
		}

		var me, other User
		db.First(&me, "id = ?", uid)
		db.First(&other, "id = ?", req.OtherUserId)
		room := ChatRoom{ID: uuid.New().String(), IsGroup: false, Participants: []User{me, other}, CreatedAt: time.Now()}
		db.Create(&room)
		return c.Status(201).JSON(fiber.Map{"roomId": room.ID})
	})

	api.Get("/chat/rooms/:roomId/messages", func(c *fiber.Ctx) error {
		var msgs []ChatMessage
		db.Preload("Sender").Where("room_id = ?", c.Params("roomId")).
			Order("created_at ASC").Find(&msgs)
		out := make([]map[string]interface{}, 0)
		for _, m := range msgs {
			out = append(out, fiber.Map{
				"id": m.ID, "senderId": m.SenderID,
				"senderUsername": m.Sender.Username,
				"text": m.Text, "voiceUrl": m.VoiceURL,
				"createdAt": m.CreatedAt.UnixMilli(),
				"readBy":    []string{},
			})
		}
		return c.JSON(out)
	})

	api.Post("/chat/rooms/:roomId/messages", func(c *fiber.Ctx) error {
		var req struct{ Text string `json:"text"` }
		c.BodyParser(&req)
		uid := currentUID(c)
		var sender User; db.First(&sender, "id = ?", uid)
		msg := ChatMessage{
			ID: uuid.New().String(),
			RoomID: c.Params("roomId"), SenderID: uid,
			Sender: sender, Text: req.Text, CreatedAt: time.Now(),
		}
		db.Create(&msg)
		db.Model(&ChatRoom{}).Where("id = ?", c.Params("roomId")).
			Updates(map[string]interface{}{"last_message_time": time.Now()})
		return c.Status(201).JSON(fiber.Map{
			"id": msg.ID, "senderId": msg.SenderID,
			"senderUsername": msg.Sender.Username,
			"text": msg.Text, "createdAt": msg.CreatedAt.UnixMilli(),
			"readBy": []string{},
		})
	})

	api.Post("/chat/rooms/:roomId/read", func(c *fiber.Ctx) error { return c.SendStatus(200) })
	api.Post("/chat/rooms/:roomId/typing", func(c *fiber.Ctx) error { return c.SendStatus(200) })

	// ── MESSAGE REQUESTS ──────────────────────────────────────────────────────
	api.Post("/chat/requests", func(c *fiber.Ctx) error {
		var req struct {
			ToUserId          string  `json:"toUserId"`
			ToUsername        string  `json:"toUsername"`
			ToProfileImageUrl *string `json:"toProfileImageUrl"`
		}
		c.BodyParser(&req)
		uid := currentUID(c)
		var me User; db.First(&me, "id = ?", uid)
		mr := MessageRequest{
			ID: uuid.New().String(),
			FromUserID: uid, ToUserID: req.ToUserId,
			FromUsername: me.Username, Status: "pending",
			CreatedAt: time.Now(),
		}
		db.Where(MessageRequest{FromUserID: uid, ToUserID: req.ToUserId}).FirstOrCreate(&mr)
		return c.SendStatus(200)
	})

	api.Get("/chat/requests/incoming", func(c *fiber.Ctx) error {
		var reqs []MessageRequest
		db.Where("to_user_id = ? AND status = 'pending'", currentUID(c)).Find(&reqs)
		out := make([]map[string]interface{}, 0)
		for _, r := range reqs {
			out = append(out, fiber.Map{
				"id": r.ID, "fromUserId": r.FromUserID,
				"fromUsername": r.FromUsername, "toUserId": r.ToUserID,
				"status": r.Status, "createdAt": r.CreatedAt.UnixMilli(),
			})
		}
		return c.JSON(out)
	})

	api.Get("/chat/requests/status", func(c *fiber.Ctx) error {
		var mr MessageRequest
		err := db.Where("from_user_id = ? AND to_user_id = ?", currentUID(c), c.Query("toUserId")).First(&mr).Error
		if err != nil { return c.JSON(nil) }
		return c.JSON(fiber.Map{"status": mr.Status})
	})

	api.Post("/chat/requests/:requestId/accept", func(c *fiber.Ctx) error {
		db.Model(&MessageRequest{}).Where("id = ?", c.Params("requestId")).Update("status", "accepted")
		return c.JSON(fiber.Map{"roomId": "room-" + c.Params("requestId")})
	})

	api.Post("/chat/requests/:requestId/decline", func(c *fiber.Ctx) error {
		db.Model(&MessageRequest{}).Where("id = ?", c.Params("requestId")).Update("status", "declined")
		return c.SendStatus(200)
	})

	// ── LOCAL NETWORK ─────────────────────────────────────────────────────────
	api.Post("/local/join",  func(c *fiber.Ctx) error { return c.SendStatus(200) })
	api.Post("/local/leave", func(c *fiber.Ctx) error { return c.SendStatus(200) })
	api.Get("/local/feed",   func(c *fiber.Ctx) error { return c.JSON([]interface{}{}) })
	api.Post("/local/posts", func(c *fiber.Ctx) error { return c.Status(201).JSON(fiber.Map{"id": "local-1"}) })
	api.Post("/local/posts/:postId/react", func(c *fiber.Ctx) error { return c.SendStatus(200) })
	api.Get("/local/people", func(c *fiber.Ctx) error { return c.JSON([]string{}) })

	// ── MEDIA ─────────────────────────────────────────────────────────────────
	api.Post("/media/voice", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{"url": "https://example.com/voice/placeholder.m4a"})
	})
	api.Post("/users/me/avatar", func(c *fiber.Ctx) error {
		var u User; db.First(&u, "id = ?", currentUID(c))
		return c.JSON(userToMap(u))
	})
	api.Post("/users/me/banner", func(c *fiber.Ctx) error {
		var u User; db.First(&u, "id = ?", currentUID(c))
		return c.JSON(userToMap(u))
	})

	// ── WEBSOCKETS ────────────────────────────────────────────────────────────
	app.Use("/ws", func(c *fiber.Ctx) error {
		if websocket.IsWebSocketUpgrade(c) { return c.Next() }
		return fiber.ErrUpgradeRequired
	})
	app.Get("/ws/chat/:userId", websocket.New(func(c *websocket.Conn) {
		userID := c.Params("userId")
		client := &Client{Conn: c, UserID: userID}
		hub.register <- client
		for {
			mt, msg, err := c.ReadMessage()
			if err != nil || mt == websocket.CloseMessage { break }
			log.Printf("WS from %s: %s", userID, string(msg))
		}
		hub.unregister <- client
	}))

	log.Fatal(app.Listen(":8080"))
}

// ── Serialisation helpers ─────────────────────────────────────────────────────

func userToMap(u User) map[string]interface{} {
	displayName := u.DisplayName
	if displayName == "" { displayName = u.Username }
	return fiber.Map{
		"id": u.ID, "username": u.Username, "phoneNumber": u.PhoneNumber,
		"displayName": displayName, "bio": u.Bio,
		"richStatus": u.RichStatus,
		"profileImageUrl": u.ProfileImageURL,
		"bannerUrl": u.BannerURL,
		"followersCount": 0, "followingCount": 0, "postsCount": 0,
		"isOnline": false, "createdAt": u.CreatedAt.UnixMilli(),
	}
}

func postToMap(p Post, caption, mediaType string) map[string]interface{} {
	if mediaType == "" { mediaType = "image" }
	if caption == "" { caption = p.Caption }
	authorUsername := ""
	authorAvatar   := ""
	if p.Author.ID != "" {
		authorUsername = p.Author.Username
		authorAvatar   = p.Author.ProfileImageURL
	}
	return fiber.Map{
		"id": p.ID, "authorId": p.AuthorID,
		"authorUsername": authorUsername,
		"authorProfileImageUrl": authorAvatar,
		"caption": caption, "imageUrl": p.ImageURL, "videoUrl": p.VideoURL,
		"mediaType": mediaType, "likesCount": 0, "commentsCount": 0,
		"createdAt": p.CreatedAt.UnixMilli(),
		"likedBy": []string{}, "reactions": map[string]string{},
	}
}

func postsToSlice(posts []Post) []map[string]interface{} {
	out := make([]map[string]interface{}, 0)
	for _, p := range posts {
		mt := "image"
		if p.VideoURL != "" {
			mt = "video"
		}
		out = append(out, postToMap(p, "", mt))
	}
	return out
}

func chatRoomToMap(r ChatRoom) map[string]interface{} {
	participants := make([]string, len(r.Participants))
	for i, p := range r.Participants { participants[i] = p.ID }
	return fiber.Map{
		"id": r.ID, "type": "direct",
		"participants": participants,
		"lastMessage": nil, "lastMessageAt": r.LastMessageTime.UnixMilli(),
		"unreadFor": []string{}, "typingUsers": []string{},
		"createdAt": r.CreatedAt.UnixMilli(),
	}
}

// Suppress unused import warning for json
var _ = json.Marshal
