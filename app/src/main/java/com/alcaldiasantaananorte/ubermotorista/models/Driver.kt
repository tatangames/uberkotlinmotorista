package com.alcaldiasantaananorte.ubermotorista.models

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Driver (
    val id: String? = null,
    val nombre: String? = null,
    val descripcion: String? = null,
    val tipo: Int? = null,
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<Driver>(json)
    }
}