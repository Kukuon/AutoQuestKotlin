package com.example.autoquest

class Offer {
    // поля офера
    // геттеры сеттеры автоматом
    var offerId: String? = null
    var brand: String? = null
    var model: String? = null
    var generation: String? = null
    var price: String? = null
    var year: String? = null
    var description: String? = null
    var enginePower: String? = null
    var fuelConsumption: String? = null
    var ownerId: String? = null
    var ownerPhoneNumber: String? = null
    private val imageUrls: List<String>? = null

    constructor() // нужен, иначе вылетает

    // конструктор для карточки объявления на главном экране
    constructor(
        brand: String?,
        model: String?,
        generation: String?,
        price: String?,
        year: String?
    ) {
        this.brand = brand
        this.model = model
        this.generation = generation
        this.price = price
        this.year = year
    }

    // основной конструктор для OfferActivity
    constructor(
        offerId: String?,
        brand: String?,
        model: String?,
        generation: String?,
        price: String?,
        year: String?,
        description: String?,
        enginePower: String?,
        fuelConsumption: String?,
        ownerId: String?,
        ownerPhoneNumber: String?
    ) {
        this.offerId = offerId
        this.brand = brand
        this.model = model
        this.generation = generation
        this.price = price
        this.year = year
        this.description = description
        this.enginePower = enginePower
        this.fuelConsumption = fuelConsumption
        this.ownerId = ownerId
        this.ownerPhoneNumber = ownerPhoneNumber
    }
}