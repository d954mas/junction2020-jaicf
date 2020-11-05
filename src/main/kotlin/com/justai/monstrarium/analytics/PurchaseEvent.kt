package com.justai.monstrarium.analytics


data class PurchaseEvent (
        val name:String,
        val entries:List<PurchaseEventEntry>
)
data class PurchaseEventEntry (
        val orderId:String,
        val price:Float,
        val currencyCode:String,
        val timestamp:Long
)
