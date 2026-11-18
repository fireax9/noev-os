package com.example.ui.cordly

import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

data class WorldLanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean,
    val hasFullTranslation: Boolean
)

@Immutable
data class AppStrings(
    val welcomeBadge: String,
    val tagline: String,
    val signInWithGoogle: String,
    val signingIn: String,
    val featureDmTitle: String,
    val featureDmSubtitle: String,
    val featureServerTitle: String,
    val featureServerSubtitle: String,
    val featureSecureTitle: String,
    val featureSecureSubtitle: String,
    val shopButton: String,
    val addButton: String,
    val codePrefix: String,
    val balanceLabel: String,
    val messagesTab: String,
    val friendsTab: String,
    val noMessagesTitle: String,
    val noMessagesSubtitle: String,
    val addFriendAction: String,
    val noFriendsTitle: String,
    val noFriendsSubtitle: String,
    val findFriendAction: String,
    val incomingRequests: String,
    val allFriendsHint: String,
    val pendingSent: String,
    val textChannelsHeader: String,
    val voiceAndVideoHeader: String,
    val voiceCallAction: String,
    val videoCallAction: String,
    val joinByNamePrefix: String,
    val copyAction: String,
    val copiedToast: String,
    val selectChatTitle: String,
    val selectChatSubtitle: String,
    val settingsTitle: String,
    val peyAvailableSuffix: String,
    val tabProfileTheme: String,
    val tabPeyShop: String,
    val tabLanguages: String,
    val backgroundTitle: String,
    val backgroundSubtitle: String,
    val yourPhoto: String,
    val bannerLabel: String,
    val displayNameLabel: String,
    val customStatusLabel: String,
    val customStatusPlaceholder: String,
    val saveProfileButton: String,
    val previewProfileButton: String,
    val signOutButton: String,
    val statusOnline: String,
    val statusIdle: String,
    val statusDnd: String,
    val statusOffline: String,
    val shopVideosTab: String,
    val shopFramesTab: String,
    val shopRewardsTitle: String,
    val dailyRewardTitle: String,
    val dailyRewardFirstSub: String,
    val dailyRewardAfterSub: String,
    val claimNowButton: String,
    val nextClaimIn: String,
    val channelRewardTitle: String,
    val channelRewardSubtitle: String,
    val copyChannelNameButton: String,
    val channelNameInputLabel: String,
    val claim200PeyButton: String,
    val channelRewardClaimedLabel: String,
    val buyButton: String,
    val equipButton: String,
    val equippedLabel: String,
    val activeLabel: String,
    val pickVideoButton: String,
    val unlockButton: String,
    val languageTitle: String,
    val languageSubtitle: String,
    val languageSearchPlaceholder: String,
    val deviceDefaultLanguage: String,
    val languagesCountSuffix: String,
    val addFriendTitle: String,
    val addFriendDesc: String,
    val addFriendInputLabel: String,
    val findFriendButton: String,
    val closeButton: String,
    val createServerTitle: String,
    val serverNameLabel: String,
    val topicOptionalLabel: String,
    val createButton: String,
    val cancelButton: String,
    val joinServerTitle: String,
    val joinServerDesc: String,
    val joinServerInputLabel: String,
    val joinButton: String,
    val createChannelTitle: String,
    val channelInputLabel: String,
    val messageHintPrefix: String,
    val replyAction: String,
    val deleteMessageAction: String,
    val replyingToPrefix: String,
    val welcomeToChannelPrefix: String,
    val startOfConversation: String,
    val aboutMember: String,
    val profileIntroLabel: String,
    val sendDirectMessageButton: String,
    val incomingVoiceCall: String,
    val incomingVideoCall: String,
    val callingStatus: String,
    val connectedCallStatus: String,
    val groupVoiceCall: String,
    val acceptCall: String,
    val declineCall: String,
    val endCall: String,
    val muteMic: String,
    val unmuteMic: String,
    val speakerOn: String,
    val cameraOn: String,
    val cameraOff: String,
    val flipCamera: String,
    val permissionCallRequired: String,
    val grantPermissions: String,
    val appearanceModeTitle: String = "Display Mode (Night & Day)",
    val appearanceModeSubtitle: String = "Switch between Night Mode (Pure Black) and bright Day Mode.",
    val nightModeLabel: String = "Night Mode",
    val dayModeLabel: String = "Day Mode",
    val appColorTitle: String = "App Color",
    val appColorSubtitle: String = "Choose your favorite accent color across neov chat.",
    val bannerStylesTitle: String = "Banner Shapes & Styles",
    val bannerStylesSubtitle: String = "Pick a geometric Discord banner or upload your own photo."
)

val EnglishStrings = AppStrings(
    welcomeBadge = "+200 P E Y WELCOME BONUS",
    tagline = "Hang out in servers, voice & video calls, and instant DMs.",
    signInWithGoogle = "Continue with Google",
    signingIn = "Signing in...",
    featureDmTitle = "Instant DMs, Voice & Video Calls",
    featureDmSubtitle = "Call & chat with friends and earn +200 P E Y per new friend",
    featureServerTitle = "Custom Servers & Join by Name",
    featureServerSubtitle = "Upload server icons and join friends' servers by typing the name",
    featureSecureTitle = "Pure Black & Google-Verified",
    featureSecureSubtitle = "AMOLED dark design secured with Google Sign-In & Firebase",
    shopButton = "Shop",
    addButton = "Add",
    codePrefix = "Code",
    balanceLabel = "Balance",
    messagesTab = "Messages",
    friendsTab = "Friends",
    noMessagesTitle = "No Direct Messages yet",
    noMessagesSubtitle = "Add a friend using Gmail or Friend Code to earn +200 P E Y, chat, and start voice or video calls.",
    addFriendAction = "Add a Friend",
    noFriendsTitle = "Your Friends list is empty",
    noFriendsSubtitle = "Add a friend to earn +200 P E Y points or claim daily & fire ax9 channel bonuses in Shop!",
    findFriendAction = "Find Friend",
    incomingRequests = "INCOMING REQUESTS",
    allFriendsHint = "ALL FRIENDS • Tap card for profile",
    pendingSent = "PENDING SENT",
    textChannelsHeader = "TEXT CHANNELS",
    voiceAndVideoHeader = "VOICE & VIDEO CALLS",
    voiceCallAction = "Voice Call",
    videoCallAction = "Video Call",
    joinByNamePrefix = "Join by Name",
    copyAction = "Copy",
    copiedToast = "Copied",
    selectChatTitle = "Select a Friend or Channel",
    selectChatSubtitle = "Choose a Direct Message, Voice/Video Call, or a Server #channel to begin.",
    settingsTitle = "neov chat Settings",
    peyAvailableSuffix = "P E Y Points",
    tabProfileTheme = "Profile",
    tabPeyShop = "P E Y Shop",
    tabLanguages = "Language",
    backgroundTitle = "App Background & Pure Black",
    backgroundSubtitle = "Enjoy Pure Black AMOLED, 3 studio wallpapers, or your own photo.",
    yourPhoto = "Your Photo",
    bannerLabel = "Banner",
    displayNameLabel = "Display Name",
    customStatusLabel = "Custom Status",
    customStatusPlaceholder = "Playing ranked / Hanging out...",
    saveProfileButton = "Save Profile & Appearance",
    previewProfileButton = "Preview Profile & 5s Video",
    signOutButton = "Sign Out",
    statusOnline = "Online",
    statusIdle = "Idle",
    statusDnd = "Do Not Disturb",
    statusOffline = "Offline",
    shopVideosTab = "Profile Videos",
    shopFramesTab = "Frames",
    shopRewardsTitle = "Free P E Y Rewards",
    dailyRewardTitle = "Daily Check-In Reward",
    dailyRewardFirstSub = "Claim +100 P E Y per day (first 2 days), then +50 P E Y every day after!",
    dailyRewardAfterSub = "Come back daily to collect +50 P E Y points!",
    claimNowButton = "Claim",
    nextClaimIn = "Next reward in",
    channelRewardTitle = "Channel Bonus: fire ax9 (+200 P E Y)",
    channelRewardSubtitle = "Copy channel name \"fire ax9\" below or paste it to unlock +200 P E Y once!",
    copyChannelNameButton = "Copy fire ax9 & Claim +200",
    channelNameInputLabel = "Type or paste: fire ax9",
    claim200PeyButton = "Claim +200 P E Y",
    channelRewardClaimedLabel = "Claimed (+200 P E Y)",
    buyButton = "Buy",
    equipButton = "Equip",
    equippedLabel = "Equipped",
    activeLabel = "Active",
    pickVideoButton = "Pick Video",
    unlockButton = "Unlock",
    languageTitle = "World Languages",
    languageSubtitle = "Choose any language in the world — updates menus, direction & calls instantly.",
    languageSearchPlaceholder = "Search any language (Arabic, French, Japanese...)",
    deviceDefaultLanguage = "Device Language (Auto)",
    languagesCountSuffix = "world languages",
    addFriendTitle = "Add a Friend",
    addFriendDesc = "Enter your friend's Gmail or neov chat Friend Code (e.g. NEV-A1B2C3) to earn +200 P E Y!",
    addFriendInputLabel = "Gmail or Friend Code",
    findFriendButton = "Find Friend",
    closeButton = "Close",
    createServerTitle = "Create a Server",
    serverNameLabel = "Server Name",
    topicOptionalLabel = "Topic (optional)",
    createButton = "Create",
    cancelButton = "Cancel",
    joinServerTitle = "Join a Server",
    joinServerDesc = "Type your friend's exact Server Name (e.g. Squad HQ) or paste an invite code.",
    joinServerInputLabel = "Server Name or Invite Code",
    joinButton = "Join",
    createChannelTitle = "Create Text Channel",
    channelInputLabel = "Channel Name",
    messageHintPrefix = "Message",
    replyAction = "Reply",
    deleteMessageAction = "Delete message",
    replyingToPrefix = "Replying to",
    welcomeToChannelPrefix = "Welcome to",
    startOfConversation = "This is the start of your conversation. Say hello below.",
    aboutMember = "ABOUT MEMBER",
    profileIntroLabel = "Profile Intro",
    sendDirectMessageButton = "Send Direct Message",
    incomingVoiceCall = "Incoming Voice Call",
    incomingVideoCall = "Incoming Video Call",
    callingStatus = "Calling...",
    connectedCallStatus = "Connected",
    groupVoiceCall = "Server Voice & Video Call",
    acceptCall = "Answer",
    declineCall = "Decline",
    endCall = "End Call",
    muteMic = "Mute",
    unmuteMic = "Unmute",
    speakerOn = "Speaker",
    cameraOn = "Cam On",
    cameraOff = "Cam Off",
    flipCamera = "Flip",
    permissionCallRequired = "Microphone & Camera permission required for voice/video calls.",
    grantPermissions = "Allow Mic & Camera",
    appearanceModeTitle = "Display Mode (Night & Day)",
    appearanceModeSubtitle = "Switch between Night Mode (Pure Black) and bright Day Mode.",
    nightModeLabel = "Night Mode",
    dayModeLabel = "Day Mode",
    appColorTitle = "App Color",
    appColorSubtitle = "Choose your favorite accent color across neov chat.",
    bannerStylesTitle = "Banner Shapes & Styles",
    bannerStylesSubtitle = "Pick a geometric Discord banner or upload your own photo."
)

val ArabicStrings = AppStrings(
    welcomeBadge = "مكافأة ترحيبية +200 P E Y",
    tagline = "تحدث مع أصدقائك، أجرِ مكالمات صوتية وفيديو، وأنشئ سيرفراتك مجاناً.",
    signInWithGoogle = "الدخول عبر حساب Google / Gmail",
    signingIn = "جاري تسجيل الدخول...",
    featureDmTitle = "رسائل فورية ومكالمات صوت وفيديو",
    featureDmSubtitle = "تواصل صوتياً وبالفيديو واكسب +200 P E Y عن كل صديق جديد",
    featureServerTitle = "سيرفرات خاصة والدخول بالاسم فقط",
    featureServerSubtitle = "خصّص صورة السيرفر وادخل سيرفر صديقك بكتابة اسمه فقط",
    featureSecureTitle = "لون أسود نقي وحماية Google",
    featureSecureSubtitle = "تصميم أسود AMOLED أنيق محمي بتسجيل دخول Google و Firebase",
    shopButton = "المتجر",
    addButton = "إضافة",
    codePrefix = "كودك",
    balanceLabel = "الرصيد",
    messagesTab = "الرسائل",
    friendsTab = "الأصدقاء",
    noMessagesTitle = "لا توجد محادثات بعد",
    noMessagesSubtitle = "أضف صديقاً عبر الجيميل أو الكود لتربح +200 P E Y وتبدأ الدردشة أو مكالمات الصوت والفيديو.",
    addFriendAction = "إضافة صديق",
    noFriendsTitle = "قائمة الأصدقاء فارغة",
    noFriendsSubtitle = "أضف صديقاً لتحصل على +200 P E Y أو احصل على المكافأة اليومية ومكافأة قناة fire ax9 من المتجر!",
    findFriendAction = "بحث عن صديق",
    incomingRequests = "طلبات الصداقة الواردة",
    allFriendsHint = "كل الأصدقاء • اضغط لعرض البروفايل",
    pendingSent = "طلبات مرسلة",
    textChannelsHeader = "القنوات النصية",
    voiceAndVideoHeader = "المكالمات الصوتية والفيديو",
    voiceCallAction = "مكالمة صوتية",
    videoCallAction = "مكالمة فيديو",
    joinByNamePrefix = "الدخول بالاسم",
    copyAction = "نسخ",
    copiedToast = "تم النسخ",
    selectChatTitle = "اختر صديقاً أو قناة",
    selectChatSubtitle = "اختر محادثة مباشرة أو مكالمة صوتية/فيديو أو قناة في السيرفر للبدء.",
    settingsTitle = "إعدادات neov chat",
    peyAvailableSuffix = "نقطة P E Y",
    tabProfileTheme = "الحساب واللون",
    tabPeyShop = "متجر P E Y",
    tabLanguages = "اللغة",
    backgroundTitle = "خلفية التطبيق واللون الأسود",
    backgroundSubtitle = "اختر اللون الأسود النقي أو إحدى الخلفيات الثلاث أو صورة من معرضك.",
    yourPhoto = "صورتك",
    bannerLabel = "البانر",
    displayNameLabel = "اسم العرض",
    customStatusLabel = "الحالة المخصصة",
    customStatusPlaceholder = "ألعب الآن / متاح للدردشة...",
    saveProfileButton = "حفظ الحساب والمظهر",
    previewProfileButton = "معاينة البروفايل وفيديو 5 ثواني",
    signOutButton = "تسجيل الخروج",
    statusOnline = "متصل",
    statusIdle = "خامل",
    statusDnd = "عدم الإزعاج",
    statusOffline = "غير متصل",
    shopVideosTab = "فيديو البروفايل",
    shopFramesTab = "أشكال البروفايل",
    shopRewardsTitle = "طرق ربح نقاط P E Y مجاناً",
    dailyRewardTitle = "المكافأة اليومية (كل يوم)",
    dailyRewardFirstSub = "احصل على +100 P E Y يومياً (أول يومين)، ثم +50 P E Y كل يوم!",
    dailyRewardAfterSub = "عُد كل يوم لتحصل على +50 P E Y تلقائياً!",
    claimNowButton = "استلام",
    nextClaimIn = "المكافأة القادمة خلال",
    channelRewardTitle = "مكافأة نسخ اسم القناة: fire ax9 (+200 P E Y)",
    channelRewardSubtitle = "انسخ اسم القناة \"fire ax9\" بالضغط أدناه أو اكتبه لتحصل على +200 P E Y فوراً!",
    copyChannelNameButton = "نسخ fire ax9 وأخذ +200",
    channelNameInputLabel = "اكتب أو الصق: fire ax9",
    claim200PeyButton = "تفعيل +200 P E Y",
    channelRewardClaimedLabel = "تم الاستلام (+200 P E Y)",
    buyButton = "شراء",
    equipButton = "تفعيل",
    equippedLabel = "مُفعّل",
    activeLabel = "نشط",
    pickVideoButton = "اختر فيديو",
    unlockButton = "فتح",
    languageTitle = "لغات العالم",
    languageSubtitle = "غيّر لغة التطبيق إلى أي لغة في العالم فوراً.",
    languageSearchPlaceholder = "ابحث عن أي لغة (العربية، الإنجليزية، الفرنسية، اليابانية...)",
    deviceDefaultLanguage = "لغة الجهاز التلقائية",
    languagesCountSuffix = "لغة عالمية",
    addFriendTitle = "إضافة صديق",
    addFriendDesc = "أدخل بريد صديقك Gmail أو كوده في neov chat لتربح +200 P E Y!",
    addFriendInputLabel = "الجيميل أو كود الصديق",
    findFriendButton = "بحث",
    closeButton = "إغلاق",
    createServerTitle = "إنشاء سيرفر جديد",
    serverNameLabel = "اسم السيرفر",
    topicOptionalLabel = "الوصف (اختياري)",
    createButton = "إنشاء",
    cancelButton = "إلغاء",
    joinServerTitle = "الدخول إلى سيرفر",
    joinServerDesc = "اكتب اسم سيرفر صديقك فقط أو رمز الدعوة للدخول فوراً.",
    joinServerInputLabel = "اسم السيرفر أو الرمز",
    joinButton = "دخول",
    createChannelTitle = "إنشاء قناة نصية",
    channelInputLabel = "اسم القناة",
    messageHintPrefix = "مراسلة",
    replyAction = "رد",
    deleteMessageAction = "حذف الرسالة",
    replyingToPrefix = "الرد على",
    welcomeToChannelPrefix = "مرحباً بك في",
    startOfConversation = "هذه بداية محادثتك. أرسل رسالتك الأولى بالأسفل.",
    aboutMember = "حول العضو",
    profileIntroLabel = "تأثير البروفايل",
    sendDirectMessageButton = "إرسال رسالة مباشرة",
    incomingVoiceCall = "مكالمة صوتية واردة",
    incomingVideoCall = "مكالمة فيديو واردة",
    callingStatus = "جاري الاتصال...",
    connectedCallStatus = "متصل الآن",
    groupVoiceCall = "مكالمة صوت وفيديو للسيرفر",
    acceptCall = "رد",
    declineCall = "رفض",
    endCall = "إنهاء",
    muteMic = "كتم",
    unmuteMic = "فتح المايك",
    speakerOn = "مكبر الصوت",
    cameraOn = "تشغيل الكاميرا",
    cameraOff = "إيقاف الكاميرا",
    flipCamera = "تبديل الكاميرا",
    permissionCallRequired = "يرجى السماح بالمايكروفون والكاميرا لإجراء المكالمات الصوتية والمرئية.",
    grantPermissions = "تفعيل المايك والكاميرا",
    appearanceModeTitle = "وضع الليل والنهار",
    appearanceModeSubtitle = "بدّل بسهولة بين الوضع الليلي الأسود ووضع النهار الفاتح.",
    nightModeLabel = "وضع الليل",
    dayModeLabel = "وضع النهار",
    appColorTitle = "لون التطبيق",
    appColorSubtitle = "اختر لون التطبيق المفضل للأزرار والمحادثات والقوائم.",
    bannerStylesTitle = "أشكال وخلفيات البانر",
    bannerStylesSubtitle = "اختر شكلاً جاهزاً للبانر أو ارفع صورتك الخاصة."
)

val FrenchStrings = EnglishStrings.copy(
    welcomeBadge = "+200 P E Y BONUS DE BIENVENUE",
    tagline = "Discutez sur vos serveurs, appels audio/vidéo et messages privés.",
    signInWithGoogle = "Continuer avec Google",
    signingIn = "Connexion...",
    shopButton = "Boutique",
    addButton = "Ajouter",
    codePrefix = "Code",
    balanceLabel = "Solde",
    messagesTab = "Messages",
    friendsTab = "Amis",
    noMessagesTitle = "Aucun message direct",
    addFriendAction = "Ajouter un ami",
    noFriendsTitle = "Liste d'amis vide",
    findFriendAction = "Trouver un ami",
    textChannelsHeader = "SALONS TEXTUELS",
    voiceAndVideoHeader = "APPELS AUDIO & VIDÉO",
    voiceCallAction = "Appel vocal",
    videoCallAction = "Appel vidéo",
    joinByNamePrefix = "Rejoindre par nom",
    copyAction = "Copier",
    copiedToast = "Copié",
    settingsTitle = "Paramètres neov chat",
    tabProfileTheme = "Profil",
    tabPeyShop = "Boutique P E Y",
    tabLanguages = "Langue",
    backgroundTitle = "Fond Noir Pur & Fonds d'écran",
    saveProfileButton = "Enregistrer",
    signOutButton = "Déconnexion",
    dailyRewardTitle = "Récompense Quotidienne",
    channelRewardTitle = "Bonus Chaîne : fire ax9 (+200 P E Y)",
    copyChannelNameButton = "Copier fire ax9 (+200 P E Y)",
    languageTitle = "Langues du monde",
    languageSearchPlaceholder = "Rechercher une langue...",
    incomingVoiceCall = "Appel vocal entrant",
    incomingVideoCall = "Appel vidéo entrant",
    callingStatus = "Appel en cours...",
    connectedCallStatus = "Connecté",
    acceptCall = "Répondre",
    declineCall = "Refuser",
    endCall = "Raccrocher",
    muteMic = "Muet",
    unmuteMic = "Réactiver",
    speakerOn = "Haut-parleur",
    cameraOn = "Caméra On",
    cameraOff = "Caméra Off",
    flipCamera = "Pivoter",
    appearanceModeTitle = "Mode Nuit et Jour",
    nightModeLabel = "Mode Nuit",
    dayModeLabel = "Mode Jour",
    appColorTitle = "Couleur de l'application"
)

val SpanishStrings = EnglishStrings.copy(
    welcomeBadge = "+200 P E Y BONO DE BIENVENIDA",
    tagline = "Chatea en servidores, llamadas de voz/video y mensajes directos.",
    signInWithGoogle = "Continuar con Google",
    signingIn = "Iniciando sesión...",
    shopButton = "Tienda",
    addButton = "Añadir",
    codePrefix = "Código",
    balanceLabel = "Saldo",
    messagesTab = "Mensajes",
    friendsTab = "Amigos",
    textChannelsHeader = "CANALES DE TEXTO",
    voiceAndVideoHeader = "LLAMADAS DE VOZ Y VIDEO",
    voiceCallAction = "Llamada de voz",
    videoCallAction = "Videollamada",
    joinByNamePrefix = "Unirse por nombre",
    copyAction = "Copiar",
    copiedToast = "Copiado",
    settingsTitle = "Ajustes de neov chat",
    tabProfileTheme = "Perfil",
    tabPeyShop = "Tienda P E Y",
    tabLanguages = "Idioma",
    backgroundTitle = "Fondo Negro Puro",
    saveProfileButton = "Guardar",
    signOutButton = "Cerrar sesión",
    dailyRewardTitle = "Recompensa Diaria",
    channelRewardTitle = "Bono Canal: fire ax9 (+200 P E Y)",
    copyChannelNameButton = "Copiar fire ax9 (+200 P E Y)",
    languageTitle = "Idiomas del mundo",
    languageSearchPlaceholder = "Buscar cualquier idioma...",
    incomingVoiceCall = "Llamada de voz entrante",
    incomingVideoCall = "Videollamada entrante",
    callingStatus = "Llamando...",
    connectedCallStatus = "Conectado",
    acceptCall = "Contestar",
    declineCall = "Rechazar",
    endCall = "Colgar",
    muteMic = "Silenciar",
    unmuteMic = "Activar mic",
    speakerOn = "Altavoz",
    cameraOn = "Cámara",
    cameraOff = "Apagar cám",
    flipCamera = "Girar",
    appearanceModeTitle = "Modo Noche y Día",
    nightModeLabel = "Modo Noche",
    dayModeLabel = "Modo Día",
    appColorTitle = "Color de la aplicación"
)

val GermanStrings = EnglishStrings.copy(
    signInWithGoogle = "Mit Google fortfahren",
    shopButton = "Shop",
    addButton = "Hinzufügen",
    messagesTab = "Nachrichten",
    friendsTab = "Freunde",
    voiceCallAction = "Sprachanruf",
    videoCallAction = "Videoanruf",
    settingsTitle = "neov chat Einstellungen",
    tabProfileTheme = "Profil",
    tabLanguages = "Sprache",
    saveProfileButton = "Speichern",
    signOutButton = "Abmelden",
    languageTitle = "Weltsprachen",
    acceptCall = "Annehmen",
    declineCall = "Ablehnen",
    endCall = "Beenden"
)

val TurkishStrings = EnglishStrings.copy(
    welcomeBadge = "+200 P E Y HOŞ GELDİN BONUSU",
    tagline = "Sunucularda buluş, sesli ve görüntülü ara, anında mesajlaş.",
    signInWithGoogle = "Google ile Devam Et",
    shopButton = "Mağaza",
    addButton = "Ekle",
    messagesTab = "Mesajlar",
    friendsTab = "Arkadaşlar",
    voiceCallAction = "Sesli Arama",
    videoCallAction = "Görüntülü Arama",
    settingsTitle = "neov chat Ayarları",
    tabProfileTheme = "Profil",
    tabLanguages = "Dil",
    saveProfileButton = "Kaydet",
    signOutButton = "Çıkış Yap",
    dailyRewardTitle = "Günlük Ödül",
    channelRewardTitle = "Kanal Bonusu: fire ax9 (+200 P E Y)",
    copyChannelNameButton = "fire ax9 Kopyala (+200 P E Y)",
    languageTitle = "Dünya Dilleri",
    acceptCall = "Yanıtla",
    declineCall = "Reddet",
    endCall = "Bitir",
    appearanceModeTitle = "Gece ve Gündüz Modu",
    nightModeLabel = "Gece Modu",
    dayModeLabel = "Gündüz Modu",
    appColorTitle = "Uygulama Rengi"
)

val PortugueseStrings = EnglishStrings.copy(
    signInWithGoogle = "Continuar com Google",
    shopButton = "Loja",
    addButton = "Adicionar",
    messagesTab = "Mensagens",
    friendsTab = "Amigos",
    voiceCallAction = "Chamada de Voz",
    videoCallAction = "Chamada de Vídeo",
    settingsTitle = "Configurações neov chat",
    tabProfileTheme = "Perfil",
    tabLanguages = "Idioma",
    saveProfileButton = "Salvar",
    signOutButton = "Sair",
    languageTitle = "Idiomas do Mundo",
    acceptCall = "Atender",
    declineCall = "Recusar",
    endCall = "Encerrar"
)

val ItalianStrings = EnglishStrings.copy(
    signInWithGoogle = "Continua con Google",
    shopButton = "Negozio",
    addButton = "Aggiungi",
    messagesTab = "Messaggi",
    friendsTab = "Amici",
    voiceCallAction = "Chiamata Vocale",
    videoCallAction = "Videochiamata",
    tabLanguages = "Lingua",
    saveProfileButton = "Salva",
    signOutButton = "Esci",
    languageTitle = "Lingue del Mondo"
)

val RussianStrings = EnglishStrings.copy(
    signInWithGoogle = "Войти через Google",
    shopButton = "Магазин",
    addButton = "Добавить",
    messagesTab = "Сообщения",
    friendsTab = "Друзья",
    voiceCallAction = "Голосовой звонок",
    videoCallAction = "Видеозвонок",
    settingsTitle = "Настройки neov chat",
    tabProfileTheme = "Профиль",
    tabLanguages = "Язык",
    saveProfileButton = "Сохранить",
    signOutButton = "Выйти",
    languageTitle = "Языки мира",
    acceptCall = "Ответить",
    declineCall = "Отклонить",
    endCall = "Завершить"
)

val JapaneseStrings = EnglishStrings.copy(
    signInWithGoogle = "Google でログイン",
    shopButton = "ショップ",
    addButton = "追加",
    messagesTab = "メッセージ",
    friendsTab = "フレンド",
    voiceCallAction = "音声通話",
    videoCallAction = "ビデオ通話",
    settingsTitle = "neov chat 設定",
    tabProfileTheme = "プロフィール",
    tabLanguages = "言語",
    saveProfileButton = "保存",
    signOutButton = "ログアウト",
    languageTitle = "世界の言語",
    acceptCall = "応答",
    declineCall = "拒否",
    endCall = "終了"
)

val KoreanStrings = EnglishStrings.copy(
    signInWithGoogle = "Google로 계속하기",
    shopButton = "상점",
    addButton = "추가",
    messagesTab = "메시지",
    friendsTab = "친구",
    voiceCallAction = "음성 통화",
    videoCallAction = "영상 통화",
    settingsTitle = "neov chat 설정",
    tabProfileTheme = "프로필",
    tabLanguages = "언어",
    saveProfileButton = "저장",
    signOutButton = "로그아웃",
    languageTitle = "세계 언어",
    acceptCall = "응답",
    declineCall = "거절",
    endCall = "종료"
)

val ChineseStrings = EnglishStrings.copy(
    signInWithGoogle = "使用 Google 登录",
    shopButton = "商店",
    addButton = "添加",
    messagesTab = "消息",
    friendsTab = "好友",
    voiceCallAction = "语音通话",
    videoCallAction = "视频通话",
    settingsTitle = "neov chat 设置",
    tabProfileTheme = "个人资料",
    tabLanguages = "语言",
    saveProfileButton = "保存",
    signOutButton = "退出登录",
    languageTitle = "世界语言",
    acceptCall = "接听",
    declineCall = "拒绝",
    endCall = "挂断"
)

val HindiStrings = EnglishStrings.copy(
    signInWithGoogle = "Google के साथ जारी रखें",
    shopButton = "दुकान",
    addButton = "जोड़ें",
    messagesTab = "संदेश",
    friendsTab = "दोस्त",
    voiceCallAction = "वॉइस कॉल",
    videoCallAction = "वीडियो कॉल",
    tabLanguages = "भाषा",
    saveProfileButton = "सहेजें",
    signOutButton = "साइन आउट",
    languageTitle = "विश्व भाषाएँ"
)

val IndonesianStrings = EnglishStrings.copy(
    signInWithGoogle = "Lanjutkan dengan Google",
    shopButton = "Toko",
    addButton = "Tambah",
    messagesTab = "Pesan",
    friendsTab = "Teman",
    voiceCallAction = "Panggilan Suara",
    videoCallAction = "Panggilan Video",
    tabLanguages = "Bahasa",
    saveProfileButton = "Simpan",
    signOutButton = "Keluar",
    languageTitle = "Bahasa Dunia"
)

val UrduStrings = EnglishStrings.copy(
    signInWithGoogle = "Google کے ساتھ جاری رکھیں",
    shopButton = "دکان",
    addButton = "شامل کریں",
    messagesTab = "پیغامات",
    friendsTab = "دوست",
    voiceCallAction = "وائس کال",
    videoCallAction = "ویڈیو کال",
    settingsTitle = "neov chat ترتیبات",
    tabProfileTheme = "پروفائل",
    tabLanguages = "زبان",
    saveProfileButton = "محفوظ کریں",
    signOutButton = "سائن آؤٹ",
    languageTitle = "دنیا کی زبانیں",
    acceptCall = "جواب دیں",
    declineCall = "انکار کریں",
    endCall = "ختم کریں"
)

val PersianStrings = EnglishStrings.copy(
    signInWithGoogle = "ادامه با Google",
    shopButton = "فروشگاه",
    addButton = "افزودن",
    messagesTab = "پیام‌ها",
    friendsTab = "دوستان",
    voiceCallAction = "تماس صوتی",
    videoCallAction = "تماس تصویری",
    settingsTitle = "تنظیمات neov chat",
    tabProfileTheme = "پروفایل",
    tabLanguages = "زبان",
    saveProfileButton = "ذخیره",
    signOutButton = "خروج",
    languageTitle = "زبان‌های جهان",
    acceptCall = "پاسخ",
    declineCall = "رد",
    endCall = "پایان"
)

object AppLanguageManager {
    private val rtlCodes = setOf("ar", "arc", "dv", "fa", "ha", "he", "iw", "khw", "ks", "ku", "ckb", "ps", "sd", "ug", "ur", "yi")

    private val translatedDictionaries: Map<String, AppStrings> = mapOf(
        "en" to EnglishStrings,
        "ar" to ArabicStrings,
        "fr" to FrenchStrings,
        "es" to SpanishStrings,
        "de" to GermanStrings,
        "tr" to TurkishStrings,
        "pt" to PortugueseStrings,
        "it" to ItalianStrings,
        "ru" to RussianStrings,
        "ja" to JapaneseStrings,
        "ko" to KoreanStrings,
        "zh" to ChineseStrings,
        "hi" to HindiStrings,
        "id" to IndonesianStrings,
        "ur" to UrduStrings,
        "fa" to PersianStrings,
        "nl" to EnglishStrings.copy(messagesTab = "Berichten", friendsTab = "Vrienden", tabLanguages = "Taal", saveProfileButton = "Opslaan", signOutButton = "Uitloggen"),
        "pl" to EnglishStrings.copy(messagesTab = "Wiadomości", friendsTab = "Znajomi", tabLanguages = "Język", saveProfileButton = "Zapisz", signOutButton = "Wyloguj"),
        "uk" to EnglishStrings.copy(messagesTab = "Повідомлення", friendsTab = "Друзі", tabLanguages = "Мова", saveProfileButton = "Зберегти", signOutButton = "Вийти"),
        "vi" to EnglishStrings.copy(messagesTab = "Tin nhắn", friendsTab = "Bạn bè", tabLanguages = "Ngôn ngữ", saveProfileButton = "Lưu", signOutButton = "Đăng xuất"),
        "th" to EnglishStrings.copy(messagesTab = "ข้อความ", friendsTab = "เพื่อน", tabLanguages = "ภาษา", saveProfileButton = "บันทึก", signOutButton = "ออกจากระบบ"),
        "bn" to EnglishStrings.copy(messagesTab = "বার্তা", friendsTab = "বন্ধুরা", tabLanguages = "ভাষা", saveProfileButton = "সংরক্ষণ"),
        "sw" to EnglishStrings.copy(messagesTab = "Ujumbe", friendsTab = "Marafiki", tabLanguages = "Lugha", saveProfileButton = "Hifadhi"),
        "el" to EnglishStrings.copy(messagesTab = "Μηνύματα", friendsTab = "Φίλοι", tabLanguages = "Γλώσσα", saveProfileButton = "Αποθήκευση"),
        "sv" to EnglishStrings.copy(messagesTab = "Meddelanden", friendsTab = "Vänner", tabLanguages = "Språk", saveProfileButton = "Spara"),
        "nb" to EnglishStrings.copy(messagesTab = "Meldinger", friendsTab = "Venner", tabLanguages = "Språk", saveProfileButton = "Lagre"),
        "da" to EnglishStrings.copy(messagesTab = "Beskeder", friendsTab = "Venner", tabLanguages = "Sprog", saveProfileButton = "Gem"),
        "fi" to EnglishStrings.copy(messagesTab = "Viestit", friendsTab = "Ystävät", tabLanguages = "Kieli", saveProfileButton = "Tallenna"),
        "cs" to EnglishStrings.copy(messagesTab = "Zprávy", friendsTab = "Přátelé", tabLanguages = "Jazyk", saveProfileButton = "Uložit"),
        "ro" to EnglishStrings.copy(messagesTab = "Mesaje", friendsTab = "Prieteni", tabLanguages = "Limbă", saveProfileButton = "Salvează"),
        "hu" to EnglishStrings.copy(messagesTab = "Üzenetek", friendsTab = "Barátok", tabLanguages = "Nyelv", saveProfileButton = "Mentés"),
        "ms" to EnglishStrings.copy(messagesTab = "Mesej", friendsTab = "Rakan", tabLanguages = "Bahasa", saveProfileButton = "Simpan"),
        "tl" to EnglishStrings.copy(messagesTab = "Mensahe", friendsTab = "Mga Kaibigan", tabLanguages = "Wika", saveProfileButton = "I-save")
    )

    val allWorldLanguages: List<WorldLanguageOption> by lazy {
        val map = linkedMapOf<String, WorldLanguageOption>()
        val priority = listOf("ar", "en", "fr", "es", "de", "tr", "pt", "it", "ru", "zh", "ja", "ko", "hi", "id", "ur", "fa")

        val localeByLang = mutableMapOf<String, Locale>()
        Locale.getAvailableLocales().forEach { loc ->
            val lang = loc.language.lowercase(Locale.ROOT)
            if (lang.length in 2..3 && lang != "und") {
                val existing = localeByLang[lang]
                if (existing == null || (existing.country.isNotBlank() && loc.country.isBlank())) {
                    localeByLang[lang] = loc
                }
            }
        }

        // Ensure priority world languages appear first cleanly
        priority.forEach { code ->
            val loc = localeByLang[code] ?: Locale.forLanguageTag(code)
            map[code] = buildOption(code, loc)
        }

        // Add ALL remaining world languages supported by Android ICU / Locale database
        localeByLang.entries
            .sortedBy { it.value.getDisplayLanguage(Locale.ENGLISH) }
            .forEach { (code, loc) ->
                if (!map.containsKey(code)) {
                    map[code] = buildOption(code, loc)
                }
            }

        map.values.toList()
    }

    private fun buildOption(code: String, locale: Locale): WorldLanguageOption {
        val nativeRaw = locale.getDisplayLanguage(locale).ifBlank { code }
        val englishRaw = locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { nativeRaw }
        val nativeCap = nativeRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        val englishCap = englishRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
        return WorldLanguageOption(
            code = code,
            nativeName = nativeCap,
            englishName = englishCap,
            isRtl = isRtlCode(code, locale),
            hasFullTranslation = translatedDictionaries.containsKey(code)
        )
    }

    fun resolveEffectiveCode(savedCode: String): String {
        if (savedCode == "system" || savedCode.isBlank()) {
            return Locale.getDefault().language.lowercase(Locale.ROOT).ifBlank { "en" }
        }
        return savedCode.lowercase(Locale.ROOT)
    }

    fun stringsFor(savedCode: String): AppStrings {
        val code = resolveEffectiveCode(savedCode)
        translatedDictionaries[code]?.let { return it }
        val baseLang = code.substringBefore('-').substringBefore('_')
        translatedDictionaries[baseLang]?.let { return it }
        return EnglishStrings
    }

    fun layoutDirectionFor(savedCode: String): LayoutDirection {
        val code = resolveEffectiveCode(savedCode)
        val locale = Locale.forLanguageTag(code)
        return if (isRtlCode(code, locale)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    private fun isRtlCode(code: String, locale: Locale): Boolean {
        val base = code.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        if (rtlCodes.contains(base)) return true
        return try {
            TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
        } catch (_: Exception) {
            false
        }
    }

    fun displayLabel(savedCode: String): String {
        if (savedCode == "system") return EnglishStrings.deviceDefaultLanguage
        val code = savedCode.lowercase(Locale.ROOT)
        return allWorldLanguages.firstOrNull { it.code == code }
            ?.let { "${it.nativeName} (${it.englishName})" }
            ?: Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).ifBlank { code }
    }
}

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
val LocalSelectedLanguageCode = staticCompositionLocalOf { "system" }
val LocalOnLanguageSelected = staticCompositionLocalOf<(String) -> Unit> { {} }
