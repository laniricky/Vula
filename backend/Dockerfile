FROM golang:1.24 AS builder

WORKDIR /app
RUN go mod init vula-backend
COPY *.go .
ENV CGO_ENABLED=0
RUN go get github.com/gofiber/fiber/v2 github.com/redis/go-redis/v9 gorm.io/gorm gorm.io/driver/postgres github.com/minio/minio-go/v7@v7.0.70 github.com/gofiber/contrib/websocket && go mod tidy
RUN go build -o main .

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/main .

EXPOSE 8080
CMD ["./main"]
