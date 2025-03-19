package com.example.filetransfer.data

data class Server(
    val id: String = "",
    val name: String,
    val address: String,
    val port: Int,
    var isSelected: Boolean = false,
    var isAvailable: Boolean = true
) {
    val fullAddress: String
        get() = "http://$address:$port"
        
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Server
        return address == other.address && port == other.port
    }
    
    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + port
        return result
    }
}