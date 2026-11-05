package com.example.sweetbean.ui.auth.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.sweetbean.R
import com.example.sweetbean.ui.common.ArtPaper
import com.example.sweetbean.ui.common.rememberNightArtFilter

@Composable
fun AuthBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.login_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = rememberNightArtFilter(ArtPaper.Login),
        modifier = modifier.fillMaxSize()
    )
}
