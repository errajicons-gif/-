# Taounate Photo Booth – Secure Email Backend

HTTPS API server for sending souvenir JPEG photos from the **Taounate Photo Booth** Android app to visitors via **Resend** or **SendGrid**, with zero photo/email disk retention.

## Endpoints

- `GET /health` — Service health & active email provider check
- `POST /send-photo` — `multipart/form-data` (`photo` JPEG <= 8 MB, `email`, `eventTitle`, `edition`)
- `POST /test-email` — JSON `{ email, eventTitle, edition }` for Organizer Settings verification

## Security & Privacy Controls

- `X-Client-Token` header checked with `crypto.timingSafeEqual`
- Strict email regex & JPEG magic-byte (`0xFF 0xD8 0xFF`) validation
- Per-IP rate limiting
- In-memory only (`multer.memoryStorage()`), zero disk storage, zero logging of emails or photo contents

## Quick Deploy (Firebase Functions v2)

```bash
cd backend
npm install
cp .env.example .env
# Fill EMAIL_PROVIDER, EMAIL_FROM, RESEND_API_KEY (or SENDGRID_API_KEY), CLIENT_APP_TOKEN
firebase deploy --only functions
```
