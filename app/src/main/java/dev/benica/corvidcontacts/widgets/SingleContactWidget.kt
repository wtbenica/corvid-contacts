// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ImageRequest
import dev.benica.corvidcontacts.CorvidContactsApplication
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.lighten
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Receiver for the [SingleContactWidget]. Handles broad android app widget lifecycle events
 * such as updates, reception additions, and instance deletions.
 */
class SingleContactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleContactWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(
            context,
            intent
        )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(
            context,
            appWidgetManager,
            appWidgetIds
        )
    }

    /**
     * Cleans up widget configuration when a widget instance is deleted from the home screen.
     */
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(
            context,
            appWidgetIds
        )
    }
}

/**
 * A Glance-based app widget that displays a chosen single contact profile
 * and maps a specific fast-interaction intent shortcut action when clicked.
 */
class SingleContactWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * Provides and displays the layout hierarchy composition content for the widget instance.
     */
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                // Reactive: re-reads whenever updateAppWidgetState() + update() runs
                val prefs = currentState<Preferences>()
                val contactId = prefs[ContactIdKey]
                val detailValue = prefs[DetailValueKey]
                val colorIntFromPrefs = prefs[ColorKey]?.toIntOrNull()
                val action = prefs[ActionKey]?.let {
                    runCatching { WidgetAction.valueOf(it) }.getOrNull()
                }

                if (contactId != null && action != null) {
                    // remember() so the Flow isn't recreated on every recomposition,
                    // but keyed on contactId so it re-subscribes if the contact changes
                    val contactFlow = remember(contactId) {
                        observeContact(
                            context,
                            contactId
                        )
                    }
                    val contact by contactFlow.collectAsState(initial = null)

                    val finalColorInt = ContactColors
                        .resolveContactColor(contact)
                        .toArgb()
                        .let { if (it == 0) null else it }
                        ?: colorIntFromPrefs

                    var avatarBitmap by remember(contactId) {
                        mutableStateOf<Bitmap?>(null)
                    }

                    LaunchedEffect(contact) {
                        avatarBitmap = contact?.contact?.photoUrl?.let { url ->
                            withContext(Dispatchers.IO) {
                                resolvePhotoToBitmap(
                                    context,
                                    url
                                )
                            }
                        }
                    }

                    contact?.let {
                        WidgetContent(
                            contact = it.contact,
                            action = action,
                            detailValue = detailValue,
                            avatarBitmap = avatarBitmap,
                            overrideColorInt = finalColorInt
                        )
                    }
                        ?: PlaceholderContent()
                } else {
                    PlaceholderContent()
                }
            }
        }
    }

    /**
     * Decodes an embedded string Base64 scheme directly, or utilizes Coil pipelines
     * to fetch external image file payloads safely into standard software Bitmaps.
     */
    private suspend fun resolvePhotoToBitmap(
        context: Context,
        photoUrl: String,
    ): Bitmap? {
        if (photoUrl.isBlank()) return null

        return try {
            if (photoUrl.startsWith("data:")) {
                // Extract raw character sequence and process decoding array conversion rules
                val base64 = photoUrl.substringAfter("base64,")
                val byteArray = Base64.decode(
                    base64,
                    Base64.DEFAULT
                )
                BitmapFactory.decodeByteArray(
                    byteArray,
                    0,
                    byteArray.size
                )
            } else {
                // Enforce safe software rendering models to bypass framework remote-view exceptions
                val imageLoader = ImageLoader(context)
                val request = ImageRequest
                    .Builder(context)
                    .data(photoUrl)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                bitmap
            }
        } catch (e: Exception) {
            Log.e(
                "SingleContactWidget",
                "Error resolving avatar image",
                e
            )
            null
        }
    }

    // Change this helper from a one-shot sync to an observable Flow
    private fun observeContact(
        context: Context,
        contactId: String,
    ): Flow<ContactWithAddressBook?> {
        val app = context.applicationContext as CorvidContactsApplication
        return app.container.contactsRepository.observeContactById(contactId)
    }

    companion object {
        // Define distinct string key constraints tracking configuration maps inside underlying data files
        private val ContactIdKey = stringPreferencesKey("contact_id")
        private val ActionKey = stringPreferencesKey("widget_action")
        private val DetailValueKey = stringPreferencesKey("detail_value")
        private val ColorKey = stringPreferencesKey("contact_color")

        /**
         * Commits target workflow properties directly into DataStore preferences and prompts UI updates.
         * Runs inside un-cancellable system blocks to prevent loss of context across configuration activity finishes.
         */
        suspend fun saveConfig(
            context: Context,
            appWidgetId: Int,
            contactId: String,
            action: WidgetAction,
            detailValue: String,
            colorInt: Int?,
        ) {
            val manager = GlanceAppWidgetManager(context)

            // Lookup tracking IDs corresponding to active system launcher layout instances
            val glanceId: GlanceId? = try {
                manager.getGlanceIdBy(appWidgetId)
            } catch (e: Exception) {
                Log.e(
                    "SingleContactWidget",
                    "Failed to get GlanceId for appWidgetId: $appWidgetId",
                    e
                )
                null
            }

            if (glanceId != null) {
                // Wrap block execution sequence in an active NonCancellable processing lane
                withContext(NonCancellable + Dispatchers.IO) {
                    try {
                        // Persist config fields into the internal preferences store file safely
                        updateAppWidgetState(
                            context,
                            PreferencesGlanceStateDefinition,
                            glanceId
                        ) { prefs ->
                            prefs
                                .toMutablePreferences()
                                .apply {
                                    set(
                                        ContactIdKey,
                                        contactId
                                    )
                                    set(
                                        ActionKey,
                                        action.name
                                    )
                                    set(
                                        DetailValueKey,
                                        detailValue
                                    )
                                    if (colorInt != null) {
                                        set(
                                            ColorKey,
                                            colorInt.toString()
                                        )
                                    } else {
                                        remove(ColorKey)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "SingleContactWidget",
                            "Error updating widget state",
                            e
                        )
                    }

                    // Perform external sanity cross-checks confirming successful preference entries
                    try {
                        // Enforce runtime update calls directly on the app widget UI layout template logic
                        SingleContactWidget().update(
                            context,
                            glanceId
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "SingleContactWidget",
                            "Error during state update or refresh",
                            e
                        )
                    }
                }
            } else {
                Log.w(
                    "SingleContactWidget",
                    "Could not get GlanceId. Is the widget still active?"
                )
            }
        }

        /**
         * Triggers a refresh for all active instances of the SingleContactWidget.
         */
        suspend fun updateAll(context: Context) {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(SingleContactWidget::class.java)
                glanceIds.forEach { id ->
                    SingleContactWidget().update(
                        context,
                        id
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "SingleContactWidget",
                    "Failed to update all widgets",
                    e
                )
            }
        }
    }
}

/**
 * Synthesizes implicit android intents based on matching selection actions and parsed structural address entries.
 */
private fun createActionIntent(
    context: Context,
    contact: ContactEntity,
    action: WidgetAction,
    detailValue: String?,
): Intent {
    return when (action) {
        WidgetAction.VIEW -> {
            Intent(Intent.ACTION_VIEW).apply {
                data = "cccontacts://contact/${contact.id}".toUri()
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        WidgetAction.CALL -> {
            // Retrieve first available telephone row entry
            val phoneNumber = detailValue ?: contact.phones?.firstOrNull()?.value ?: ""
            Intent(Intent.ACTION_DIAL).apply {
                data = "tel:${
                    Uri.encode(
                        phoneNumber,
                        "+()-. "
                    )
                }".toUri()
            }
        }

        WidgetAction.TEXT -> {
            // Build standard target direct SMS intent URI
            val phoneNumber = detailValue ?: contact.phones?.firstOrNull()?.value ?: ""
            Intent(Intent.ACTION_VIEW).apply {
                data = "sms:${
                    Uri.encode(
                        phoneNumber,
                        "+()-. "
                    )
                }".toUri()
            }
        }

        WidgetAction.EMAIL -> {
            // Bind target to direct mail transmission schemes (encode @ and other special chars)
            val email = detailValue ?: contact.emails?.firstOrNull()?.value ?: ""
            Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:${Uri.encode(email)}".toUri()
            }
        }

        WidgetAction.DIRECTIONS -> {
            // Combine multi-tier address variables into flattened search terms before map query encoding passes
            val address = detailValue ?: contact.structuredAddresses
                ?.firstOrNull()
                ?.let { addressItem ->
                    listOfNotNull(
                        addressItem.street,
                        addressItem.city,
                        addressItem.state,
                        addressItem.postalCode,
                        addressItem.country
                    ).joinToString(", ")
                }
            //                    ?: contact.address
            ?: ""
            Intent(Intent.ACTION_VIEW).apply {
                data = "geo:0,0?q=${Uri.encode(address)}".toUri()
            }
        }
    }
}

/**
 * Composable content layout mapping for the widget surface when setup configuration properties exist.
 */
@Composable
private fun WidgetContent(
    contact: ContactEntity,
    action: WidgetAction,
    detailValue: String?,
    avatarBitmap: Bitmap?,
    overrideColorInt: Int? = null,
) {
    val size = LocalSize.current
    val squareSize = minOf(
        size.width,
        size.height
    )

    val context = LocalContext.current

    // Build communication system route based on the selected click target
    val intent = remember(
        contact,
        action,
        detailValue
    ) {
        try {
            createActionIntent(
                context,
                contact,
                action,
                detailValue
            )
        } catch (e: Exception) {
            Log.e(
                "SingleContactWidget",
                "Error creating action intent",
                e
            )
            Intent()
        }
    }

    // Render clicking background node wrapper matching built action pathway
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center
    ) {
        GlanceContactAvatar(
            displayName = contact.getEffectiveDisplayName(),
            bitmap = avatarBitmap,
            colorInt = overrideColorInt ?: contact.colorInt,
            action = action,
            modifier = GlanceModifier.size(squareSize)
        )
    }
}

/**
 * Composable content shown when the widget needs initial user personalization configuration parameters.
 */
@Composable
fun PlaceholderContent() {
    val context = LocalContext.current

    Text(
        text = context.getString(R.string.widget_placeholder),
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )
    )
}

/**
 * A customized Composable rendering a circular contact thumbnail with an overlaid activity type badge indicator.
 */
@Composable
private fun GlanceContactAvatar(
    displayName: String,
    bitmap: Bitmap?,
    colorInt: Int?,
    action: WidgetAction,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current

    val backgroundColorProvider: ColorProvider = if (colorInt != null) {
        val color = Color(colorInt)
        ColorProvider(
            day = color,
            night = color
        )
    } else {
        GlanceTheme.colors.primary
    }

    val surfaceColorProvider: ColorProvider = if (colorInt != null) {
        val color = Color(colorInt).lighten(0.8f)
        ColorProvider(
            day = color,
            night = color
        )
    } else {
        val themePrimary = GlanceTheme.colors.primary
            .getColor(context)
            .lighten(0.4f)
        ColorProvider(
            day = themePrimary,
            night = themePrimary
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColorProvider)
                .cornerRadius(Dimens.cornerRadius)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(R.string.widget_photo_description),
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(Dimens.cornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(surfaceColorProvider)
                        .cornerRadius(Dimens.cornerRadius),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName
                            .firstOrNull()
                            ?.uppercase() ?: "?",
                        style = TextStyle(
                            color = backgroundColorProvider,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Resolve context badge asset links corresponding to target behaviors
        val iconRes = when (action) {
            WidgetAction.CALL -> R.drawable.ic_call
            WidgetAction.TEXT -> R.drawable.ic_message
            WidgetAction.EMAIL -> R.drawable.ic_email
            WidgetAction.DIRECTIONS -> R.drawable.ic_directions
            WidgetAction.VIEW -> R.drawable.ic_person
        }

        // Render action icon badge indicator on bottom right edge corner
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(Dimens.cornerRadius)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
            )
        }
    }
}

enum class WidgetAction {
    CALL,
    TEXT,
    EMAIL,
    DIRECTIONS,
    VIEW
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(
    widthDp = 100,
    heightDp = 200
)
@Composable
fun PlaceholderContentPreview() {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(Dimens.cornerRadius),
            contentAlignment = Alignment.Center
        ) {
            PlaceholderContent()
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
fun GlanceContactAvatarPreview() {
    GlanceTheme {
        GlanceContactAvatar(
            displayName = "John Doe",
            bitmap = null,
            colorInt = null,
            action = WidgetAction.CALL,
            modifier = GlanceModifier.size(100.dp)
        )
    }
}