package main

import (
	"context"
	"crypto/rand"
	"fmt"
	"log"
	"math/big"
	"os"
	"time"

	"github.com/gofiber/contrib/websocket"
	"github.com/gofiber/fiber/v2"
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

func initRedis() {
	rdb = redis.NewClient(&redis.Options{
		Addr: "redis:6379",
	})
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

	// Auto Migrate the models
	err = db.AutoMigrate(&User{}, &Post{}, &Reaction{}, &ChatRoom{}, &ChatMessage{})
	if err != nil {
		log.Fatal("Failed to migrate database:", err)
	}
	log.Println("Connected to PostgreSQL and Migrated Schema")
}

func initMinio() {
	endpoint := os.Getenv("MINIO_ENDPOINT")
	if endpoint == "" {
		endpoint = "minio:9000"
	}
	accessKey := os.Getenv("MINIO_ROOT_USER")
	secretKey := os.Getenv("MINIO_ROOT_PASSWORD")
	if accessKey == "" {
		accessKey = "vula_admin"
		secretKey = "secure_password"
	}

	var err error
	minioC, err = minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(accessKey, secretKey, ""),
		Secure: false,
	})
	if err != nil {
		log.Println("Failed to initialize MinIO Client:", err)
	} else {
		log.Println("Connected to MinIO")
		
		// Create buckets if they don't exist
		buckets := []string{"profiles", "posts", "stories", "chat-media"}
		for _, b := range buckets {
			exists, _ := minioC.BucketExists(ctx, b)
			if !exists {
				minioC.MakeBucket(ctx, b, minio.MakeBucketOptions{})
			}
		}
	}
}

func main() {
	initRedis()
	initDB()
	initMinio()

	hub = newHub()
	go hub.run()

	app := fiber.New()
	app.Use(logger.New())

	app.Get("/health", func(c *fiber.Ctx) error {
		return c.SendString("Vula Backend is OK")
	})

	// --- AUTHENTICATION ---
	auth := app.Group("/api/auth")
	auth.Post("/request-code", func(c *fiber.Ctx) error {
		type Request struct {
			Phone string `json:"phone"`
		}
		var req Request
		if err := c.BodyParser(&req); err != nil {
			return c.Status(400).JSON(fiber.Map{"error": "Invalid request"})
		}

		n, _ := rand.Int(rand.Reader, big.NewInt(900000))
		code := fmt.Sprintf("%06d", n.Int64()+100000)

		if rdb != nil {
			rdb.Set(ctx, "otp:"+req.Phone, code, 5*time.Minute)
		}

		log.Printf("====> SMS: Your Vula code is %s <====", code)
		return c.JSON(fiber.Map{"message": "Code sent successfully"})
	})

	auth.Post("/verify-code", func(c *fiber.Ctx) error {
		type Request struct {
			Phone string `json:"phone"`
			Code  string `json:"code"`
		}
		var req Request
		if err := c.BodyParser(&req); err != nil {
			return c.Status(400).JSON(fiber.Map{"error": "Invalid request"})
		}

		if rdb != nil {
			val, err := rdb.Get(ctx, "otp:"+req.Phone).Result()
			if err != nil || val != req.Code {
				return c.Status(400).JSON(fiber.Map{"error": "Invalid code"})
			}
			rdb.Del(ctx, "otp:"+req.Phone)
		}

		// Find or Create user in Postgres
		var user User
		result := db.Where("phone_number = ?", req.Phone).First(&user)
		if result.Error != nil {
			user = User{PhoneNumber: req.Phone}
			db.Create(&user)
		}

		return c.JSON(fiber.Map{
			"message": "Authentication successful",
			"token":   "mock-jwt-for-" + user.ID,
		})
	})

	// --- WEBSOCKETS ---
	// Middleware to check if it's a websocket request
	app.Use("/ws", func(c *fiber.Ctx) error {
		if websocket.IsWebSocketUpgrade(c) {
			// In a real app, you'd extract the user ID from the JWT token here
			// c.Locals("userID", extractedID)
			return c.Next()
		}
		return fiber.ErrUpgradeRequired
	})

	app.Get("/ws/chat/:userId", websocket.New(func(c *websocket.Conn) {
		// In a real app, use c.Locals("userID") from middleware auth
		userID := c.Params("userId") 
		
		client := &Client{
			Conn:   c,
			UserID: userID,
		}
		
		hub.register <- client
		
		// Wait for messages from this client (if any)
		for {
			mt, msg, err := c.ReadMessage()
			if err != nil || mt == websocket.CloseMessage {
				break
			}
			log.Printf("Received message from %s: %s", userID, string(msg))
			
			// Here you would normally save the message to Postgres, then broadcast it to the room
			// using hub.SendMessage(recipientId, chatMessageJSON)
		}
		
		hub.unregister <- client
	}))

	log.Fatal(app.Listen(":8080"))
}
