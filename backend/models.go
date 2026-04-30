package main

import (
	"time"

	"gorm.io/gorm"
)

// User represents a user profile
type User struct {
	ID              string `gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
	PhoneNumber     string `gorm:"uniqueIndex"`
	Username        string `gorm:"uniqueIndex"`
	ProfileImageURL string
	Bio             string
	FCMToken        string
	CreatedAt       time.Time
	UpdatedAt       time.Time
}

// Post represents a feed post or story
type Post struct {
	ID        string `gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
	AuthorID  string `gorm:"type:uuid;index"`
	Author    User   `gorm:"foreignKey:AuthorID"`
	ImageURL  string
	VideoURL  string
	IsStory   bool `gorm:"default:false"`
	CreatedAt time.Time
}

// Reaction represents an emoji reaction on a Post
type Reaction struct {
	PostID    string `gorm:"primaryKey;type:uuid"`
	UserID    string `gorm:"primaryKey;type:uuid"`
	Emoji     string
	CreatedAt time.Time
	
	Post Post `gorm:"foreignKey:PostID"`
	User User `gorm:"foreignKey:UserID"`
}

// ChatRoom represents a conversation thread
type ChatRoom struct {
	ID              string `gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
	IsGroup         bool   `gorm:"default:false"`
	Name            string
	LastMessageTime time.Time
	Participants    []User `gorm:"many2many:chat_room_participants;"`
	CreatedAt       time.Time
}

// ChatMessage represents a single message within a ChatRoom
type ChatMessage struct {
	ID          string `gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
	RoomID      string `gorm:"type:uuid;index"`
	Room        ChatRoom `gorm:"foreignKey:RoomID"`
	SenderID    string `gorm:"type:uuid"`
	Sender      User `gorm:"foreignKey:SenderID"`
	Text        string
	VoiceURL    string
	CreatedAt   time.Time
	ReadBy      []User `gorm:"many2many:message_read_receipts;"`
}
