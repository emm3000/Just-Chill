package com.emm.justchill.core.error

import com.emm.domain.auth.MIN_SIGNUP_PASSWORD_LENGTH
import com.emm.domain.recurring.MAX_DAY_OF_MONTH
import com.emm.domain.recurring.MIN_DAY_OF_MONTH
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

fun DomainException.toUserMessage(): String = when (this) {
    is DomainException.NotFound -> "No encontré eso"
    is DomainException.ValidationError -> code.toUserMessage()
    is DomainException.DatabaseError -> "Hubo un problema guardando tu data"
    is DomainException.Unauthorized -> "Credenciales incorrectas o sesión expirada"
    is DomainException.NetworkUnavailable -> "Sin conexión — revisa tu internet"
    is DomainException.Unknown -> "Algo se rompió — capaz reinicia la app?"
}

/**
 * The only place a [ValidationCode] becomes words the user reads.
 *
 * A `ValidationError`'s own `message` is English and diagnostic; it never reaches the snackbar.
 * Keep every branch on one line — a multiline branch makes ktlint demand a blank line between all
 * nineteen of them.
 *
 * Suppressed: this is a flat dispatch table over an enum, so every case added raises the cyclomatic
 * count by one while the code stays exactly as simple as it was. Splitting it to satisfy the
 * threshold would trade the compiler's exhaustiveness check for nothing.
 */
@Suppress("CyclomaticComplexMethod")
private fun ValidationCode.toUserMessage(): String = when (this) {
    ValidationCode.NameRequired -> "El nombre no puede estar vacío"
    ValidationCode.AccountRequired -> "Selecciona una cuenta"
    ValidationCode.AmountRequired -> "Ingresa un monto"
    ValidationCode.AmountMustBePositive -> "El monto debe ser mayor a cero"
    ValidationCode.DateInTheFuture -> "No puedes registrar un movimiento con fecha futura"
    ValidationCode.DayOfMonthOutOfRange -> "El día del mes debe estar entre $MIN_DAY_OF_MONTH y $MAX_DAY_OF_MONTH"
    ValidationCode.RecurringAlreadyConfirmed -> "Ya confirmaste este movimiento para este mes"
    ValidationCode.AccountHasTransactions -> "Esta cuenta tiene movimientos. Bórralos o muévelos antes de eliminarla"
    ValidationCode.AccountHasRecurringMovements -> "Esta cuenta tiene recurrentes activos. Elimínalos primero"
    ValidationCode.EmailInvalid -> "Revisa el correo — no parece válido"
    ValidationCode.EmailAlreadyRegistered -> "Ese correo ya tiene una cuenta. Inicia sesión"
    ValidationCode.PasswordRequired -> "Ingresa tu contraseña"
    ValidationCode.PasswordTooShort -> "La contraseña necesita al menos $MIN_SIGNUP_PASSWORD_LENGTH caracteres"
    ValidationCode.PasswordTooWeak -> "Esa contraseña es muy débil. Combina letras, números y símbolos"
    ValidationCode.PasswordUnchanged -> "La contraseña nueva es igual a la anterior"
    ValidationCode.GoogleTokenInvalid -> "No pudimos validar tu cuenta de Google — intenta de nuevo"
    ValidationCode.BackupFileInvalid -> "El archivo está dañado o no es un respaldo de JustChill"
    ValidationCode.BackupVersionUnsupported -> "Ese respaldo es de una versión que esta app no puede leer"
    ValidationCode.BackupUploadUnverified -> "No pudimos verificar tu respaldo en la nube — intenta de nuevo"
    ValidationCode.Unspecified -> "Algo no cuadra con los datos"
}
