package main

import "time"

// User represents a user profile
type User struct {
	ID              string `gorm:"primaryKey"`
	PhoneNumber     string `gorm:"uniqueIndex"`
	Username        string `gorm:"uniqueIndex"`
	DisplayName     string
	Bio             string
	RichStatus      string
	ProfileImageURL string
	BannerURL       string
	FCMToken        string
	CreatedAt       time.Time
	UpdatedAt       time.Time
}

// Post represents a feed post
type Post struct {
	ID        string `gorm:"primaryKey"`
	AuthorID  string `gorm:"index"`
	Author    User   `gorm:"foreignKey:AuthorID"`
	Caption   string
	ImageURL  string
	VideoURL  string
	IsStory   bool      `gorm:"default:false"`
	CreatedAt time.Time
}

// Story represents a 24-hour story
type Story struct {
	ID        string `gorm:"primaryKey"`
	AuthorID  string `gorm:"index"`
	Author    User   `gorm:"foreignKey:AuthorID"`
	ImageURL  string
	VideoURL  string
	CreatedAt time.Time
	ExpiresAt time.Time
}

// Reaction represents an emoji reaction on a Post
type Reaction struct {
	PostID    string `gorm:"primaryKey"`
	UserID    string `gorm:"primaryKey"`
	Emoji     string
	CreatedAt time.Time
	Post      Post `gorm:"foreignKey:PostID"`
	User      User `gorm:"foreignKey:UserID"`
}

// Follow represents a follower → followed relationship
type Follow struct {
	FollowerID string    `gorm:"primaryKey"`
	FollowedID string    `gorm:"primaryKey"`
	Follower   User      `gorm:"foreignKey:FollowerID"`
	Followed   User      `gorm:"foreignKey:FollowedID"`
	CreatedAt  time.Time
}

// ChatRoom represents a conversation thread
type ChatRoom struct {
	ID              string    `gorm:"primaryKey"`
	IsGroup         bool      `gorm:"default:false"`
	Name            string
	LastMessageTime time.Time
	Participants    []User    `gorm:"many2many:chat_room_participants;"`
	CreatedAt       time.Time
}

// ChatMessage represents a single message within a ChatRoom
type ChatMessage struct {
	ID        string    `gorm:"primaryKey"`
	RoomID    string    `gorm:"index"`
	Room      ChatRoom  `gorm:"foreignKey:RoomID"`
	SenderID  string
	Sender    User      `gorm:"foreignKey:SenderID"`
	Text      string
	VoiceURL  string
	CreatedAt time.Time
	ReadBy    []User    `gorm:"many2many:message_read_receipts;"`
}

// MessageRequest represents a request to start a direct conversation
type MessageRequest struct {
	ID           string    `gorm:"primaryKey"`
	FromUserID   string    `gorm:"index"`
	ToUserID     string    `gorm:"index"`
	FromUsername string
	Status       string    `gorm:"default:'pending'"` // pending | accepted | declined
	CreatedAt    time.Time
}
