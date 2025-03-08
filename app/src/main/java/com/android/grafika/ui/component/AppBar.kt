package com.android.grafika.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.grafika.R
import com.android.grafika.ui.theme.GrafikaTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrafikaTopAppBar(
    title: String,
    onNavigationIconClick: () -> Unit = {},
    showNavIcon: Boolean = false,
    showIconInTitle: Boolean = true,
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIconInTitle) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "Grafika Logo"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(title)
            }
        },

        expandedHeight = 56.dp,
        actions = actions,
        navigationIcon = {
            if (showNavIcon) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun GrafikaTopAppBarPreview() {
    GrafikaTheme {
        GrafikaTopAppBar(
            title = "Grafika",
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
            }
        )
    }
}