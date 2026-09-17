package com.emm.justchill.core.database.backup

internal data class DecodedBackup(val declaredVersion: Int, val payload: ExportPayloadDto)
