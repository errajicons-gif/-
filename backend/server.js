const crypto = require("crypto");
const express = require("express");
const cors = require("cors");
const multer = require("multer");

const MAX_IMAGE_BYTES = 8 * 1024 * 1024; // 8 MB JPEG limit
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

// Lightweight in-memory rate limiter (per IP, 10-minute window)
const rateBuckets = new Map();
const RATE_WINDOW_MS = 10 * 60 * 1000;
const RATE_MAX_REQUESTS = 30;

function checkRateLimit(ip) {
  const now = Date.now();
  const entry = rateBuckets.get(ip);
  if (!entry || now > entry.resetAt) {
    rateBuckets.set(ip, { count: 1, resetAt: now + RATE_WINDOW_MS });
    return true;
  }
  if (entry.count >= RATE_MAX_REQUESTS) {
    return false;
  }
  entry.count += 1;
  return true;
}

function timingSafeMatch(provided, expected) {
  if (!expected) return true; // Token check skipped only if CLIENT_APP_TOKEN is unset
  if (!provided || typeof provided !== "string") return false;
  const a = Buffer.from(provided);
  const b = Buffer.from(expected);
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

function isValidJpegBuffer(buffer) {
  if (!buffer || buffer.length < 4) return false;
  return buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff;
}

function buildEmailContent(eventTitle, edition) {
  const subject = "صورتك التذكارية من ملتقى التوجيه 12 بتاونات";
  const textBody = [
    "مرحباً بك،",
    "",
    "يسعدنا مشاركتك في ملتقى التوجيه المدرسي والمهني في دورته الثانية عشرة بمدينة تاونات.",
    "",
    "نرسل إليك صورتك التذكارية التي التقطتها خلال هذه المناسبة.",
    "",
    "شكراً لزيارتك، ونتمنى لك تجربة مميزة.",
    "",
    "ملتقى التوجيه المدرسي والمهني – تاونات."
  ].join("\n");

  const htmlBody = `
  <div dir="rtl" style="font-family: Tahoma, Arial, sans-serif; background:#f4f6fa; padding:24px; color:#0b1f48;">
    <div style="max-width:600px; margin:auto; background:#ffffff; border:2px solid #d4af37; border-radius:16px; overflow:hidden;">
      <div style="background:#0b1f48; color:#f6de85; padding:20px 24px; text-align:center;">
        <div style="font-size:12px; letter-spacing:2px; font-weight:bold;">TAOUNATE PHOTO BOOTH</div>
        <h2 style="margin:8px 0 4px; color:#ffffff;">${escapeHtml(eventTitle || "ملتقى التوجيه المدرسي والمهني")}</h2>
        <div style="font-size:14px;">${escapeHtml(edition || "الدورة الثانية عشرة – تاونات")}</div>
      </div>
      <div style="padding:24px; line-height:1.9; font-size:16px;">
        <p>مرحباً بك،</p>
        <p>يسعدنا مشاركتك في ملتقى التوجيه المدرسي والمهني في دورته الثانية عشرة بمدينة تاونات.</p>
        <p>نرسل إليك صورتك التذكارية التي التقطتها خلال هذه المناسبة (مرفقة بصيغة JPEG).</p>
        <p>شكراً لزيارتك، ونتمنى لك تجربة مميزة.</p>
        <p style="font-weight:bold; color:#0b1f48;">ملتقى التوجيه المدرسي والمهني – تاونات.</p>
      </div>
    </div>
  </div>`;

  return { subject, textBody, htmlBody };
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

async function sendViaResend({ to, subject, text, html, attachmentBase64 }) {
  const apiKey = process.env.RESEND_API_KEY;
  const from = process.env.EMAIL_FROM;
  if (!apiKey || !from) {
    throw new Error("Server email configuration incomplete (RESEND_API_KEY / EMAIL_FROM).");
  }

  const payload = {
    from,
    to: [to],
    subject,
    text,
    html
  };

  if (attachmentBase64) {
    payload.attachments = [
      {
        filename: "taounate-forum-12-souvenir.jpg",
        content: attachmentBase64
      }
    ];
  }

  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || `Email provider rejected request (${res.status})`);
  }
  return data.id || "sent";
}

async function sendViaSendGrid({ to, subject, text, html, attachmentBase64 }) {
  const apiKey = process.env.SENDGRID_API_KEY;
  const from = process.env.EMAIL_FROM;
  if (!apiKey || !from) {
    throw new Error("Server email configuration incomplete (SENDGRID_API_KEY / EMAIL_FROM).");
  }

  const payload = {
    personalizations: [{ to: [{ email: to }] }],
    from: { email: from, name: "Taounate Photo Booth" },
    subject,
    content: [
      { type: "text/plain", value: text },
      { type: "text/html", value: html }
    ]
  };

  if (attachmentBase64) {
    payload.attachments = [
      {
        content: attachmentBase64,
        filename: "taounate-forum-12-souvenir.jpg",
        type: "image/jpeg",
        disposition: "attachment"
      }
    ];
  }

  const res = await fetch("https://api.sendgrid.com/v3/mail/send", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    throw new Error(`SendGrid delivery failed (${res.status})`);
  }
  return res.headers.get("x-message-id") || "sent";
}

async function dispatchEmail(params) {
  const provider = (process.env.EMAIL_PROVIDER || "resend").toLowerCase();
  if (provider === "sendgrid") {
    return sendViaSendGrid(params);
  }
  return sendViaResend(params);
}

function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: "1mb" }));

  // Store uploaded JPEG strictly in RAM and discard immediately after sending (zero retention)
  const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: MAX_IMAGE_BYTES }
  });

  app.get("/health", (_req, res) => {
    const provider = (process.env.EMAIL_PROVIDER || "resend").toLowerCase();
    res.json({
      ok: true,
      service: "taounate-photo-booth-backend",
      provider
    });
  });

  app.post("/send-photo", upload.single("photo"), async (req, res) => {
    try {
      const clientIp = req.ip || req.headers["x-forwarded-for"] || "unknown";
      if (!checkRateLimit(String(clientIp))) {
        return res.status(429).json({
          success: false,
          error: "تم تجاوز الحد المسموح من الطلبات مؤقتاً. يرجى المحاولة بعد قليل."
        });
      }

      const providedToken = req.header("X-Client-Token");
      if (!timingSafeMatch(providedToken, process.env.CLIENT_APP_TOKEN)) {
        return res.status(401).json({
          success: false,
          error: "غير مصرح باستخدام هذه الواجهة."
        });
      }

      const recipientEmail = String(req.body?.email || "").trim();
      if (!EMAIL_REGEX.test(recipientEmail) || recipientEmail.length > 254) {
        return res.status(400).json({
          success: false,
          error: "عنوان البريد الإلكتروني غير صالح."
        });
      }

      const file = req.file;
      if (!file || !isValidJpegBuffer(file.buffer)) {
        return res.status(400).json({
          success: false,
          error: "ملف الصورة المرفق غير صالح أو مفقود (مطلوب JPEG)."
        });
      }

      const { subject, textBody, htmlBody } = buildEmailContent(
        req.body?.eventTitle,
        req.body?.edition
      );

      let attachmentBase64 = file.buffer.toString("base64");
      // Clear reference to raw upload buffer as soon as encoded
      file.buffer = null;

      const messageId = await dispatchEmail({
        to: recipientEmail,
        subject,
        text: textBody,
        html: htmlBody,
        attachmentBase64
      });

      attachmentBase64 = null;

      return res.status(200).json({
        success: true,
        message: "تم إرسال الصورة بنجاح.",
        messageId
      });
    } catch (err) {
      // Never log visitor emails, API keys, or image content
      console.error("Email dispatch failed:", err.message);
      return res.status(502).json({
        success: false,
        error: "تعذر إرسال البريد الإلكتروني عبر مزود الخدمة حالياً."
      });
    }
  });

  app.post("/test-email", async (req, res) => {
    try {
      const providedToken = req.header("X-Client-Token");
      if (!timingSafeMatch(providedToken, process.env.CLIENT_APP_TOKEN)) {
        return res.status(401).json({ success: false, error: "غير مصرح." });
      }

      const recipientEmail = String(req.body?.email || "").trim();
      if (!EMAIL_REGEX.test(recipientEmail)) {
        return res.status(400).json({ success: false, error: "بريد الاختبار غير صالح." });
      }

      const { subject, textBody, htmlBody } = buildEmailContent(
        req.body?.eventTitle,
        req.body?.edition
      );

      const messageId = await dispatchEmail({
        to: recipientEmail,
        subject: `[اختبار] ${subject}`,
        text: textBody,
        html: htmlBody,
        attachmentBase64: null
      });

      return res.status(200).json({
        success: true,
        message: "تم إرسال البريد التجريبي بنجاح.",
        messageId
      });
    } catch (err) {
      console.error("Test email failed:", err.message);
      return res.status(502).json({
        success: false,
        error: "فشل إرسال البريد التجريبي."
      });
    }
  });

  // Handle Multer payload size error cleanly
  app.use((err, _req, res, _next) => {
    if (err && err.code === "LIMIT_FILE_SIZE") {
      return res.status(413).json({
        success: false,
        error: "حجم الصورة يتجاوز الحد الأقصى المسموح به (8 ميجابايت)."
      });
    }
    return res.status(500).json({
      success: false,
      error: "حدث خطأ غير متوقع في الخادم."
    });
  });

  return app;
}

if (require.main === module) {
  const port = process.env.PORT || 8080;
  createApp().listen(port, () => {
    console.log(`Taounate Photo Booth backend listening on port ${port}`);
  });
}

module.exports = { createApp };
