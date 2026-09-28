package com.ostarosto.app.feature.address

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.SavedAddress

/** Local-only, on-device address book — add/edit/delete named delivery locations. */
@Composable
fun ManageAddressesSheet(
    addresses: List<SavedAddress>,
    onAddNew: () -> Unit,
    onEdit: (SavedAddress) -> Unit,
    onDelete: (SavedAddress) -> Unit,
    onSetDefault: (SavedAddress) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            Ar.savedAddressesHeading,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OstaColors.Ink,
        )
        Spacer(Modifier.height(12.dp))

        if (addresses.isEmpty()) {
            Text(
                Ar.noSavedAddresses,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        } else {
            addresses.forEach { address ->
                AddressRow(
                    address = address,
                    onEdit = { onEdit(address) },
                    onDelete = { onDelete(address) },
                    onSetDefault = { onSetDefault(address) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        OutlinedButton(onClick = onAddNew, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(Ar.addAddress)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AddressRow(
    address: SavedAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault) OstaColors.MaroonTint else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OstaColors.Maroon)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onSetDefault)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        address.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OstaColors.Ink,
                    )
                    if (address.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50), color = OstaColors.Maroon, contentColor = Color.White) {
                            Text(
                                Ar.defaultAddressBadge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (!address.addressText.isNullOrBlank()) {
                    Text(
                        address.addressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = Ar.editAddress, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = Ar.deleteAddress,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
