package main

import (
	"log"
	"sync"

	"github.com/gofiber/contrib/websocket"
)

// Client represents a connected user
type Client struct {
	Conn   *websocket.Conn
	UserID string // Extracted from JWT
}

// Hub maintains the set of active clients
type Hub struct {
	// Registered clients mapped by UserID
	clients map[string]map[*Client]bool
	
	// Register requests from the clients
	register chan *Client
	
	// Unregister requests from clients
	unregister chan *Client
	
	mu sync.RWMutex
}

func newHub() *Hub {
	return &Hub{
		clients:    make(map[string]map[*Client]bool),
		register:   make(chan *Client),
		unregister: make(chan *Client),
	}
}

func (h *Hub) run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			if _, ok := h.clients[client.UserID]; !ok {
				h.clients[client.UserID] = make(map[*Client]bool)
			}
			h.clients[client.UserID][client] = true
			h.mu.Unlock()
			log.Printf("User %s connected via WebSocket", client.UserID)

		case client := <-h.unregister:
			h.mu.Lock()
			if connections, ok := h.clients[client.UserID]; ok {
				if _, ok := connections[client]; ok {
					delete(connections, client)
					client.Conn.Close()
					if len(connections) == 0 {
						delete(h.clients, client.UserID)
					}
				}
			}
			h.mu.Unlock()
			log.Printf("User %s disconnected", client.UserID)
		}
	}
}

// SendMessage sends a payload to a specific connected user
func (h *Hub) SendMessage(userID string, message interface{}) {
	h.mu.RLock()
	defer h.mu.RUnlock()
	
	if connections, ok := h.clients[userID]; ok {
		for client := range connections {
			err := client.Conn.WriteJSON(message)
			if err != nil {
				log.Printf("Error sending message to %s: %v", userID, err)
				client.Conn.Close()
			}
		}
	}
}
