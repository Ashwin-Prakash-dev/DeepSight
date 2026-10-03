# Router: is this field the kind of image the chosen pack expects?

The engine runs the router on every field that passes the quality gate, before the pack model
(`FieldPipeline`: quality → router → pack). It never picks a pack. The user picks; the router only says
match, mismatch (with the pack it looks like) or reject. A mismatch or reject blocks the field, and triage
then gives `NEEDS_EXPERT` (`engine.router_mismatch` / `engine.router_reject`, contracts/README.md).

| File | What |
|---|---|
| `router.onnx` | ResNet18, opset 17. Input `input` float32 `[N,3,224,224]`, RGB in 0..1; mean/std are inside. Outputs `probs` (softmax, already temperature-scaled), `emb`, `maha_percentile`; the app reads `probs` only |
| `labels.json` | Pack id of each `probs` column: `malaria_thin, fungal, leukaemia_wbc, breast_breakhis, reject` |
| `router_meta.json` | Everything the notebook recorded: preprocessing rule, sources, versions, config, `model_sha256` (the app refuses a model that differs) |
| `eval_report.md` | The notebook's measured evaluation (summary below) |
| `golden/` | 7 cases from the run (`expected.json`, `inputs_u8.bin`), `blank.png`/`noise.png`, and two Pillow resize references (`pil_bilinear_*.png`) |

**In the app:** `RouterModel.fromAssets` loads it from `assets/router/` (staged by `:app`'s `stagePacks`),
checks the sha256 and the labels, and `guardFor(packId)` gives `ScoreRouterGuard` for the four packs above.
Any other pack (the smoke test pack) keeps `AlwaysMatchRouterGuard`. `CaseRunner` loads it once per process; the
debug analyse screen shows the verdict.

**Preprocessing** (`RouterInput`, from `router_meta.json` `resize_rule`): EXIF orientation (CaseRunner's decode),
shorter side to 256 with Pillow's bilinear resize (`PilBilinear`, a port of Pillow's Resample.c), long side
`floor(long * 256 / short)`, centre crop 224 at floor offsets, divide by 255. No mean/std and no softmax in the app.

## Provenance

Trained 2026-10-03 with `folder/deepsight_router_colab.ipynb` on Colab (torch 2.11.0+cu130, onnxruntime 1.30.0,
Pillow 11.3.0; all versions in `router_meta.json`). ImageNet ResNet18, 1 probe + 3 fine-tune epochs, temperature
fitted on grouped validation data. Sources (600 images per source at most, 5 per patient/slide/source image):

| Class | Sources |
|---|---|
| malaria_thin | NIH-NLM thin smears (193 patients), NIH-NLM thick smears Pf (150 patients) |
| fungal | DeFungi (416 source images) |
| leukaemia_wbc | Taleqani, Kaggle `mehradaria/leukemia` (no patient IDs: 1 group per image), C-NMC, Kaggle `andrewmvd/leukemia-classification` (73 patients) |
| breast_breakhis | BreakHis (80 slides). BACH was meant to be a second source but the notebook's filter missed its `Photos/` folder |
| reject | Oxford pets, Flowers-102, DTD textures, Kather colorectal tissue (50 tiles, 10 slides), synthetic images |

## Measured (eval_report.md; none of this is clinical)

- **B, unseen patients/slides of the training sources:** 98.8% of 1,119 images (malaria 99.6, fungal 100,
  leukaemia 100, breast 97.5, reject 98.0). Leukaemia's figure may be optimistic: Taleqani has no patient IDs.
- **A, whole sources it never saw:** 59.6% of 1,015. NLM thick smears 600/600 → malaria. C-NMC leukaemia
  0/365 (346 reject, 19 malaria; C-NMC is single-cell crops, training was fields). Kather tissue, which should
  be rejected, 40/50 → breast. Disease-vs-reject AUROC 0.42–0.61.
- **Repo images (desktop, 2026-10-03):** all 23 NIH-NLM demo/parity fields → malaria_thin (0.987–1.000); the 6
  breast goldens → breast_breakhis; the blank placeholder goldens of fungal/leukaemia → reject. These are training
  sources, so this checks the demo, not generalisation.
- **Phone (edge 50 fusion, `RouterModelDeviceTest`, 2026-10-03):** the 7 golden cases match PyTorch within
  5e-10 on CPU and XNNPACK; blank/noise PNGs match end to end; an RBCNet field (5312x2988) matches on malaria_thin
  (0.997) and is blocked on breast_breakhis as `malaria_thin`. Router step 371 ms per field warm, 831 ms on the
  first field (session creation).

**UNVERIFIED:** real phone-camera photos of any pack (every one is a source the router never saw); Android JPEG
decoding versus Pillow's (PNG input is exact); any other tissue or stain being routed to breast or fungal; the
leukaemia class beyond Taleqani-like fields.

## Licences

Private repo only (AGENTS.md exception, [LICENSING.md](../../LICENSING.md)). The weights derive from BreakHis
(non-commercial wording), C-NMC (CC BY-NC 4.0 as described upstream), Taleqani (licence UNVERIFIED), NIH-NLM,
DeFungi, Kather (CC BY 4.0), Oxford pets/flowers/DTD (terms UNVERIFIED) and ImageNet-pretrained ResNet18.
The golden tensors are derived from those images too.

## Regenerating the golden fixtures

From the repo root, with the Colab run's `golden_cases.npz` in `folder/`: the 7 cases are indices 0, 5, 10, 15,
20, 25, 26 (one per class, then blank and noise), saved as `rint(x * 255)` uint8 (the inputs are exactly
uint8/255). The resize references are `pattern(w, h).resize((dw, dh), Image.Resampling.BILINEAR)` with the
hash pattern in `PilBilinearTest.pattern`, made with Pillow 12.3.0.
