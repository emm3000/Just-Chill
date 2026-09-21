package com.emm.justchill.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun CommitFooter(commitHash: String, onCopyClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val commit: CommitHashUi = commitHashUi(commitHash)) {
            is CommitHashUi.Available -> CopyableCommitRow(
                label = commit.label,
                onCopyClick = onCopyClick,
            )

            CommitHashUi.Unavailable -> Text(
                text = commit.label,
                style = type.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = spacing.s4, vertical = spacing.s2),
            )
        }
    }
}

@Composable
private fun CopyableCommitRow(label: String, onCopyClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    Row(
        modifier = Modifier
            .clip(radii.rXS)
            .clickable(onClick = onCopyClick)
            .semantics { role = Role.Button }
            .heightIn(min = spacing.s12)
            .padding(horizontal = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = label,
            style = type.caption,
            color = colors.textTertiary,
        )
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copiar el hash completo del commit",
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s3),
        )
    }
}
