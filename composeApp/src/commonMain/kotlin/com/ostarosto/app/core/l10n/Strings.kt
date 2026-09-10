package com.ostarosto.app.core.l10n

/**
 * Arabic UI strings. The app is Arabic-only for the MVP, so this is a plain
 * object rather than a resource-qualifier system. Move to compose-resources
 * or moko-resources if a second locale is ever added.
 */
object Ar {
    const val appName = "أسطى روستو"

    // Auth
    const val enterPhone = "أدخل رقم هاتفك"
    const val phoneHint = "رقم الهاتف"
    const val sendCode = "إرسال الرمز"
    const val enterCode = "أدخل رمز التحقق"
    const val codeSentTo = "تم إرسال رمز إلى"
    const val verify = "تأكيد"
    const val resendCode = "إعادة إرسال الرمز"
    const val resendIn = "إعادة الإرسال خلال"
    const val seconds = "ثانية"
    const val yourName = "اسمك"
    const val saveAndContinue = "حفظ ومتابعة"
    const val invalidCode = "رمز غير صحيح"

    // Menu
    const val menu = "المنيو"
    const val chooseBranch = "اختر الفرع"
    const val branchClosed = "الفرع مغلق حالياً"
    const val openNow = "مفتوح الآن"
    const val closedNow = "مغلق"
    const val currentBranch = "الفرع الحالي"
    const val viewCart = "عرض السلة"
    const val items = "عنصر"
    const val search = "ابحث عن منتج"
    const val noProducts = "لا توجد منتجات"
    const val outOfStock = "غير متوفر"
    const val addToCart = "أضف إلى السلة"
    const val currency = "ج.م"

    // Cart / checkout
    const val cart = "السلة"
    const val emptyCart = "سلتك فارغة"
    const val subtotal = "الإجمالي الفرعي"
    const val discount = "الخصم"
    const val tax = "الضريبة"
    const val deliveryFee = "رسوم التوصيل"
    const val total = "الإجمالي"
    const val checkout = "إتمام الطلب"
    const val pickup = "استلام من الفرع"
    const val delivery = "توصيل"
    const val deliveryAddress = "عنوان التوصيل"
    const val orderNotes = "ملاحظات الطلب"
    const val paymentMethod = "طريقة الدفع"
    const val placeOrder = "تأكيد الطلب"
    const val orderPlaced = "تم استلام طلبك"

    // Card payment
    const val redirectingToPayment = "جارٍ تحويلك إلى صفحة الدفع…"
    const val waitingForPayment = "في انتظار تأكيد الدفع"
    const val waitingForPaymentHint = "أكمل الدفع في المتصفح ثم عُد إلى التطبيق."
    const val iHavePaid = "لقد أتممت الدفع"
    const val reopenPayment = "إعادة فتح صفحة الدفع"
    const val paymentFailed = "فشلت عملية الدفع"
    const val couldNotOpenPayment = "تعذّر فتح صفحة الدفع. تأكد من وجود متصفح ثم حاول مجدداً."
    const val backToCart = "العودة إلى السلة"

    // Profile / tabs
    const val profile = "حسابي"
    const val loyaltyPoints = "نقاط الولاء"
    const val phoneLabel = "رقم الهاتف"

    // Orders
    const val myOrders = "طلباتي"
    const val orderNumber = "رقم الطلب"
    const val trackOrder = "تتبع الطلب"
    const val cancelOrder = "إلغاء الطلب"
    const val cancelOrderConfirmTitle = "إلغاء الطلب"
    const val cancelOrderConfirmBody = "هل أنت متأكد من إلغاء هذا الطلب؟ لا يمكن التراجع."
    const val confirm = "تأكيد"
    const val dismiss = "تراجع"
    const val orderItems = "عناصر الطلب"
    const val orderSummary = "ملخص الطلب"
    const val noOrders = "لا توجد طلبات بعد"

    // Progress steps
    const val stepReceived = "تم الاستلام"
    const val stepPreparing = "قيد التحضير"
    const val stepReady = "جاهز"
    const val stepOnTheWay = "في الطريق إليك"
    const val stepCompleted = "تم التسليم"
    const val stepCancelled = "أُلغي"

    // Generic
    const val back = "رجوع"
    const val retry = "إعادة المحاولة"
    const val somethingWentWrong = "حدث خطأ ما"
    const val noConnection = "لا يوجد اتصال بالإنترنت"
    const val logout = "تسجيل الخروج"
}
