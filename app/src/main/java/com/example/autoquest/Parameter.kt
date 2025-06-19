package com.example.autoquest

class Parameter {
    // поля отдельного параметра в офере
    var name: String? = null
    var value: String? = null


    // необходимый пустой конструктор
    constructor()

    constructor(name: String?, value: String?) {
        this.name = name
        this.value = value
    }
}

