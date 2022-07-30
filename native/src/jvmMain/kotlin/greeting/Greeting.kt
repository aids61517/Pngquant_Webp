package greeting

actual class Greeting {
    actual fun greeting(): String {
        return "Guess what it is! > Jvm!"
    }
}