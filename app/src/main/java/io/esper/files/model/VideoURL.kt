package io.esper.files.model

@Suppress("unused")
class VideoURL {
    var name: String? = null
    var url: String? = null

    constructor()
    constructor(
        name: String?,
        url: String?,

        ) {
        this.name = name
        this.url = url
    }
}