# TweeterClone

A functional social networking mobile application for Android, developed as part of an engineering thesis.

## 🛠 System Architecture
- **Client:** Android application written in Java (API Level 24+).
- **Backend:** REST API developed in PHP 8.1.
- **Database:** MySQL 8.0 with a relational schema.
- **Design Patterns:** MVC, Singleton, and Adapter patterns.

## ✨ Key Features
- **Security:** Secure authentication using bcrypt password hashing, reCAPTCHA Enterprise bot protection, and strong password enforcement (min. 8 characters, uppercase, lowercase, digit, special character).
- **End-to-End Encryption:** Direct messages are encrypted with AES-256-GCM. The AES key is wrapped with the recipient's RSA-2048 public key (stored server-side); the private key never leaves the Android Keystore. Each user has their own unique key pair.
- **Interactions:** Creating tweets with images, likes, and comments.
- **Privacy:** Follow system with support for private profiles and manual approval of follow requests.
- **Messaging:** Direct messaging restricted to mutual followers, with end-to-end encrypted content and decrypted previews in the conversation list.
- **Input Validation:** Email format validated client-side before submission; password rules shown inline with per-field error hints.
- **UI:** Modern interface following Material Design with Light/Dark mode support.
- **Server Logging:** All API actions are logged to daily rotating files (`logs/api_YYYY-MM-DD.log`) — writes, errors, unauthorized attempts, and security events — without logging any encrypted or personal content.

## 📚 Libraries Used
- **Volley:** Networking and API communication.
- **Glide:** Image loading and caching.
- **CircleImageView:** Circular UI elements for profile pictures.
- **reCAPTCHA Enterprise:** Bot protection on login and registration.
