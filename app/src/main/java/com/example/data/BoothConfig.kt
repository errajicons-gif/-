package com.example.data

data class BoothConfig(
    val brandHeader: String = "TAOUNATE PHOTO BOOTH",
    val eventTitle: String = "ملتقى التوجيه المدرسي والمهني",
    val editionLabel: String = "الدورة الثانية عشرة – تاونات",
    val mainHeadline: String = "خلّد مشاركتك في ملتقى التوجيه 12 بتاونات",
    val welcomePhrase: String = "لحظتك في الملتقى… ذكرى تبقى.",
    val footerCaption: String = "التقط صورتك، واحتفظ بذكرى مشاركتك، واستقبلها عبر بريدك الإلكتروني",
    val backendBaseUrl: String = "",
    val clientToken: String = "",
    val inactivityTimeoutSeconds: Int = 60,
    val customLogoPath: String? = null,
    val customPortraitFramePath: String? = null,
    val customLandscapeFramePath: String? = null
)
