// use an integer for version numbers
version = 1

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Turkish series with English subtitles from Turkish123"
    authors = listOf("YourUsername")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=hds.turkish123.com&sz=%size%"
}
