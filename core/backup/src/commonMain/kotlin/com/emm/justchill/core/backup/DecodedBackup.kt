package com.emm.justchill.core.backup

data class DecodedBackup(val declaredVersion: Int, val payload: ExportPayloadDto)
