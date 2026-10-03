package com.deepsight.batch

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.deepsight.data.CaseStatus
import com.deepsight.profiles.PatientProfile
import com.deepsight.ui.DeepSightIcons
import com.deepsight.ui.ThemePreviews
import com.deepsight.ui.components.EmptyState
import com.deepsight.ui.components.NoticeRow
import com.deepsight.ui.components.SectionHeader
import com.deepsight.ui.components.decodeDisplayBitmap
import com.deepsight.ui.theme.DeepSightTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Batch upload and the batches it queued. Picked images go to the router, which proposes a test for each ([upload]);
 * the user reviews the proposal and submits one batch per test to the shared queue. Batch-tab submissions are listed
 * in queue order; a finished batch opens for sign-off ([onOpen]).
 */
@Composable
fun BatchScreen(
    batches: List<BatchItem>,
    onOpen: (String) -> Unit,
    onUseSingle: () -> Unit,
    modifier: Modifier = Modifier,
    upload: UploadUiState = UploadUiState.Idle,
    patients: List<PatientProfile> = emptyList(),
    patientUid: String? = null,
    onPickImages: (List<Uri>) -> Unit = {},
    onPatient: (String?) -> Unit = {},
    onSubmit: () -> Unit = {},
    onDiscard: () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UploadCard(upload, onPickImages)
        if (upload is UploadUiState.Review) Review(upload.plan, patients, patientUid, onPatient, onSubmit, onDiscard)
        SectionHeader("Batches", supporting = if (batches.isEmpty()) null else "Run one at a time, in the order they were submitted.")
        if (batches.isEmpty()) EmptyState(DeepSightIcons.Batch, title = "No batches yet", body = "Batches you run will be listed here.")
        batches.forEach { BatchRow(it, onOpen) }
        TextButton(onClick = onUseSingle, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Screen one case in Single") }
    }
}

@Composable
private fun UploadCard(upload: UploadUiState, onPickImages: (List<Uri>) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> if (uris.isNotEmpty()) onPickImages(uris) }
    val busy = upload is UploadUiState.Copying || upload is UploadUiState.Sorting
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.medium) {
                Icon(DeepSightIcons.Batch, contentDescription = null, Modifier.padding(10.dp).size(28.dp))
            }
            Text("Upload fields", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Pick field images from any tests. The router sorts them by test, and you check the result before anything runs.",
                style = MaterialTheme.typography.bodyMedium,
            )
            when (upload) {
                is UploadUiState.Copying -> Progress("Copying ${images(upload.total)}…", null)
                is UploadUiState.Sorting -> Progress("Sorting image ${upload.done} of ${upload.total}…", upload.done to upload.total)
                is UploadUiState.Failed -> Text(upload.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
                Icon(DeepSightIcons.Gallery, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select images")
            }
        }
    }
}

@Composable
private fun Progress(text: String, fraction: Pair<Int, Int>?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        else LinearProgressIndicator(progress = { (fraction.first - 0.5f).coerceAtLeast(0f) / fraction.second }, modifier = Modifier.fillMaxWidth())
    }
}

/** The router's proposal: what will run as which test, and what is skipped. Nothing runs until Submit. */
@Composable
private fun Review(
    plan: BatchPlan,
    patients: List<PatientProfile>,
    patientUid: String?,
    onPatient: (String?) -> Unit,
    onSubmit: () -> Unit,
    onDiscard: () -> Unit,
) {
    SectionHeader("Sorted by the router", supporting = "Check the tests before submitting. Each test's images become one batch.")
    plan.groups.forEach { group -> ImageGroup("${group.packName} · ${images(group.images.size)}", group.images) }
    if (plan.rejected.isNotEmpty()) ImageGroup("Not recognised as a slide · ${images(plan.rejected.size)} (skipped)", plan.rejected, muted = true)
    plan.notInstalled.groupBy { it.label }.forEach { (label, list) ->
        ImageGroup("Looks like $label, not installed · ${images(list.size)} (skipped)", list, muted = true)
    }
    PatientChoice(patients, patientUid, onPatient)
    NoticeRow("The router proposes the test; each field is checked again by its test before analysis.", DeepSightIcons.Info)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onSubmit, enabled = plan.groups.isNotEmpty()) {
            Text(if (plan.groups.size == 1) "Submit 1 batch" else "Submit ${plan.groups.size} batches")
        }
        OutlinedButton(onClick = onDiscard) { Text("Discard") }
    }
}

@Composable
private fun ImageGroup(title: String, images: List<RoutedImage>, muted: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (muted) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                images.forEach { Thumbnail(it) }
            }
        }
    }
}

@Composable
private fun Thumbnail(image: RoutedImage) {
    val bitmap by produceState<Bitmap?>(null, image.file) { value = withContext(Dispatchers.IO) { decodeDisplayBitmap(image.file, maxSide = 192) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Text("%.0f%%".format(image.score * 100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Optional: the patient every submitted batch belongs to. Patients are added on the Profiles screen. */
@Composable
private fun PatientChoice(patients: List<PatientProfile>, patientUid: String?, onPatient: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val chosen = patients.firstOrNull { it.id == patientUid }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Icon(DeepSightIcons.Person, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(chosen?.let { "Patient: ${it.name} · ${it.id}" } ?: "Patient: none")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onPatient(null); open = false })
            patients.forEach { p -> DropdownMenuItem(text = { Text("${p.name} · ${p.id}") }, onClick = { onPatient(p.id); open = false }) }
        }
    }
}

private fun images(n: Int) = if (n == 1) "1 image" else "$n images"

@Composable
private fun BatchRow(item: BatchItem, onOpen: (String) -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.patient ?: "No patient", style = MaterialTheme.typography.titleMedium)
            Text(item.packName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                item.statusLine(),
                style = MaterialTheme.typography.labelLarge,
                color = if (item.status == CaseStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            item.progress?.let { (done, total) ->
                if (total > 0) LinearProgressIndicator(progress = { (done - 0.5f).coerceAtLeast(0f) / total }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
    }
    if (item.status == CaseStatus.DONE) Card(onClick = { onOpen(item.caseId) }, colors = colors, modifier = Modifier.fillMaxWidth()) { content() }
    else Card(colors = colors, modifier = Modifier.fillMaxWidth()) { content() }
}

@ThemePreviews
@Composable
private fun BatchScreenPreview() = DeepSightTheme {
    BatchScreen(
        listOf(
            BatchItem("c1", "Ada Example · P-0000-0001", "Malaria (thin smear)", CaseStatus.RUNNING, 2 to 4, null, null),
            BatchItem("c2", "Ben Sample · P-0000-0002", "Malaria (thin smear)", CaseStatus.QUEUED, null, 1, null),
        ),
        onOpen = {}, onUseSingle = {},
        upload = UploadUiState.Review(
            BatchPlan(
                groups = listOf(BatchPlan.Group("malaria_thin", "Malaria (thin smear)", listOf(RoutedImage(File("a"), "malaria_thin", 0.99)))),
                rejected = listOf(RoutedImage(File("b"), "reject", 0.97)),
                notInstalled = emptyList(),
            ),
        ),
    )
}
