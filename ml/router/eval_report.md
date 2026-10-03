# Measured router evaluation

No test images were used for training, temperature fitting or threshold selection.

## A
Images: 1015; accuracy: 0.596059
ECE before: 0.373666; after: 0.390448
Confusion matrix (rows true, columns predicted): malaria, fungal, leukaemia, breast_cancer, reject
```json
[[600, 0, 0, 0, 0], [0, 0, 0, 0, 0], [19, 0, 0, 0, 346], [0, 0, 0, 0, 0], [0, 5, 0, 40, 5]]
```
### Per class
```json
{
  "malaria": {
    "n": 600,
    "accuracy": 1.0
  },
  "fungal": {
    "n": 0,
    "accuracy": null
  },
  "leukaemia": {
    "n": 365,
    "accuracy": 0.0
  },
  "breast_cancer": {
    "n": 0,
    "accuracy": null
  },
  "reject": {
    "n": 50,
    "accuracy": 0.1
  }
}
```
### Per source
```json
{
  "nlm_thick_pf": {
    "n": 600,
    "accuracy": 1.0
  },
  "cnmc": {
    "n": 365,
    "accuracy": 0.0
  },
  "kather": {
    "n": 50,
    "accuracy": 0.1
  }
}
```
### Disease versus reject AUROC
```json
{
  "1-p(reject)": 0.42375129533678757,
  "max_softmax": 0.6053471502590675,
  "mahalanobis": 0.4541554404145078
}
```
### Top probability percentiles
```json
{
  "malaria": {
    "0": 0.9959668148716461,
    "5": 0.9999692437247917,
    "25": 0.9999980243416422,
    "50": 0.9999993939016467,
    "75": 0.9999998383597698,
    "95": 0.9999999725478602,
    "100": 0.9999999938181188
  },
  "leukaemia": {
    "0": 0.538997975608298,
    "5": 0.8007271439796682,
    "25": 0.99386087499363,
    "50": 0.999855492046747,
    "75": 0.999998992572773,
    "95": 0.9999999982959531,
    "100": 0.9999999999956546
  },
  "reject": {
    "0": 0.5088277151120666,
    "5": 0.5879969738184325,
    "25": 0.8952792480291046,
    "50": 0.9999876927755766,
    "75": 0.999999872176883,
    "95": 0.9999999998498867,
    "100": 0.9999999999998539
  }
}
```
### Coverage versus accuracy
```json
[
  {
    "probability": 0.0,
    "margin": 0.0,
    "accepted": 664,
    "coverage": 0.6541871921182266,
    "accuracy": 0.9036144578313253,
    "disease_coverage": 0.6414507772020726,
    "reject_false_accept": 0.9
  },
  {
    "probability": 0.0,
    "margin": 0.1,
    "accepted": 662,
    "coverage": 0.6522167487684729,
    "accuracy": 0.9063444108761329,
    "disease_coverage": 0.6414507772020726,
    "reject_false_accept": 0.86
  },
  {
    "probability": 0.0,
    "margin": 0.2,
    "accepted": 660,
    "coverage": 0.6502463054187192,
    "accuracy": 0.9090909090909091,
    "disease_coverage": 0.6404145077720207,
    "reject_false_accept": 0.84
  },
  {
    "probability": 0.0,
    "margin": 0.3,
    "accepted": 658,
    "coverage": 0.6482758620689655,
    "accuracy": 0.9118541033434651,
    "disease_coverage": 0.6393782383419689,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.5,
    "margin": 0.0,
    "accepted": 664,
    "coverage": 0.6541871921182266,
    "accuracy": 0.9036144578313253,
    "disease_coverage": 0.6414507772020726,
    "reject_false_accept": 0.9
  },
  {
    "probability": 0.5,
    "margin": 0.1,
    "accepted": 662,
    "coverage": 0.6522167487684729,
    "accuracy": 0.9063444108761329,
    "disease_coverage": 0.6414507772020726,
    "reject_false_accept": 0.86
  },
  {
    "probability": 0.5,
    "margin": 0.2,
    "accepted": 660,
    "coverage": 0.6502463054187192,
    "accuracy": 0.9090909090909091,
    "disease_coverage": 0.6404145077720207,
    "reject_false_accept": 0.84
  },
  {
    "probability": 0.5,
    "margin": 0.3,
    "accepted": 658,
    "coverage": 0.6482758620689655,
    "accuracy": 0.9118541033434651,
    "disease_coverage": 0.6393782383419689,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.7,
    "margin": 0.0,
    "accepted": 657,
    "coverage": 0.6472906403940887,
    "accuracy": 0.91324200913242,
    "disease_coverage": 0.6383419689119171,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.7,
    "margin": 0.1,
    "accepted": 657,
    "coverage": 0.6472906403940887,
    "accuracy": 0.91324200913242,
    "disease_coverage": 0.6383419689119171,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.7,
    "margin": 0.2,
    "accepted": 657,
    "coverage": 0.6472906403940887,
    "accuracy": 0.91324200913242,
    "disease_coverage": 0.6383419689119171,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.7,
    "margin": 0.3,
    "accepted": 657,
    "coverage": 0.6472906403940887,
    "accuracy": 0.91324200913242,
    "disease_coverage": 0.6383419689119171,
    "reject_false_accept": 0.82
  },
  {
    "probability": 0.8,
    "margin": 0.0,
    "accepted": 651,
    "coverage": 0.6413793103448275,
    "accuracy": 0.9216589861751152,
    "disease_coverage": 0.6341968911917099,
    "reject_false_accept": 0.78
  },
  {
    "probability": 0.8,
    "margin": 0.1,
    "accepted": 651,
    "coverage": 0.6413793103448275,
    "accuracy": 0.9216589861751152,
    "disease_coverage": 0.6341968911917099,
    "reject_false_accept": 0.78
  },
  {
    "probability": 0.8,
    "margin": 0.2,
    "accepted": 651,
    "coverage": 0.6413793103448275,
    "accuracy": 0.9216589861751152,
    "disease_coverage": 0.6341968911917099,
    "reject_false_accept": 0.78
  },
  {
    "probability": 0.8,
    "margin": 0.3,
    "accepted": 651,
    "coverage": 0.6413793103448275,
    "accuracy": 0.9216589861751152,
    "disease_coverage": 0.6341968911917099,
    "reject_false_accept": 0.78
  },
  {
    "probability": 0.9,
    "margin": 0.0,
    "accepted": 641,
    "coverage": 0.6315270935960591,
    "accuracy": 0.9360374414976599,
    "disease_coverage": 0.627979274611399,
    "reject_false_accept": 0.7
  },
  {
    "probability": 0.9,
    "margin": 0.1,
    "accepted": 641,
    "coverage": 0.6315270935960591,
    "accuracy": 0.9360374414976599,
    "disease_coverage": 0.627979274611399,
    "reject_false_accept": 0.7
  },
  {
    "probability": 0.9,
    "margin": 0.2,
    "accepted": 641,
    "coverage": 0.6315270935960591,
    "accuracy": 0.9360374414976599,
    "disease_coverage": 0.627979274611399,
    "reject_false_accept": 0.7
  },
  {
    "probability": 0.9,
    "margin": 0.3,
    "accepted": 641,
    "coverage": 0.6315270935960591,
    "accuracy": 0.9360374414976599,
    "disease_coverage": 0.627979274611399,
    "reject_false_accept": 0.7
  },
  {
    "probability": 0.95,
    "margin": 0.0,
    "accepted": 637,
    "coverage": 0.6275862068965518,
    "accuracy": 0.9419152276295133,
    "disease_coverage": 0.6248704663212435,
    "reject_false_accept": 0.68
  },
  {
    "probability": 0.95,
    "margin": 0.1,
    "accepted": 637,
    "coverage": 0.6275862068965518,
    "accuracy": 0.9419152276295133,
    "disease_coverage": 0.6248704663212435,
    "reject_false_accept": 0.68
  },
  {
    "probability": 0.95,
    "margin": 0.2,
    "accepted": 637,
    "coverage": 0.6275862068965518,
    "accuracy": 0.9419152276295133,
    "disease_coverage": 0.6248704663212435,
    "reject_false_accept": 0.68
  },
  {
    "probability": 0.95,
    "margin": 0.3,
    "accepted": 637,
    "coverage": 0.6275862068965518,
    "accuracy": 0.9419152276295133,
    "disease_coverage": 0.6248704663212435,
    "reject_false_accept": 0.68
  },
  {
    "probability": 0.99,
    "margin": 0.0,
    "accepted": 634,
    "coverage": 0.6246305418719211,
    "accuracy": 0.9463722397476341,
    "disease_coverage": 0.6227979274611399,
    "reject_false_accept": 0.66
  },
  {
    "probability": 0.99,
    "margin": 0.1,
    "accepted": 634,
    "coverage": 0.6246305418719211,
    "accuracy": 0.9463722397476341,
    "disease_coverage": 0.6227979274611399,
    "reject_false_accept": 0.66
  },
  {
    "probability": 0.99,
    "margin": 0.2,
    "accepted": 634,
    "coverage": 0.6246305418719211,
    "accuracy": 0.9463722397476341,
    "disease_coverage": 0.6227979274611399,
    "reject_false_accept": 0.66
  },
  {
    "probability": 0.99,
    "margin": 0.3,
    "accepted": 634,
    "coverage": 0.6246305418719211,
    "accuracy": 0.9463722397476341,
    "disease_coverage": 0.6227979274611399,
    "reject_false_accept": 0.66
  }
]
```

## B
Images: 1119; accuracy: 0.988382
ECE before: 0.007348; after: 0.006930
Confusion matrix (rows true, columns predicted): malaria, fungal, leukaemia, breast_cancer, reject
```json
[[235, 0, 0, 0, 1], [0, 124, 0, 0, 0], [0, 0, 189, 0, 0], [0, 0, 0, 78, 2], [0, 4, 0, 6, 480]]
```
### Per class
```json
{
  "malaria": {
    "n": 236,
    "accuracy": 0.9957627118644068
  },
  "fungal": {
    "n": 124,
    "accuracy": 1.0
  },
  "leukaemia": {
    "n": 189,
    "accuracy": 1.0
  },
  "breast_cancer": {
    "n": 80,
    "accuracy": 0.975
  },
  "reject": {
    "n": 490,
    "accuracy": 0.9795918367346939
  }
}
```
### Per source
```json
{
  "nlm_thin": {
    "n": 116,
    "accuracy": 0.9913793103448276
  },
  "nlm_thick_pf": {
    "n": 120,
    "accuracy": 1.0
  },
  "defungi": {
    "n": 124,
    "accuracy": 1.0
  },
  "breakhis": {
    "n": 80,
    "accuracy": 0.975
  },
  "taleqani": {
    "n": 119,
    "accuracy": 1.0
  },
  "cnmc": {
    "n": 70,
    "accuracy": 1.0
  },
  "pets": {
    "n": 120,
    "accuracy": 1.0
  },
  "flowers": {
    "n": 120,
    "accuracy": 0.9916666666666667
  },
  "dtd": {
    "n": 120,
    "accuracy": 0.925
  },
  "kather": {
    "n": 10,
    "accuracy": 1.0
  },
  "synthetic": {
    "n": 120,
    "accuracy": 1.0
  }
}
```
### Disease versus reject AUROC
```json
{
  "1-p(reject)": 0.9987605853152072,
  "max_softmax": 0.3850167093864572,
  "mahalanobis": 0.9983290613542715
}
```
### Top probability percentiles
```json
{
  "malaria": {
    "0": 0.4633870878098528,
    "5": 0.9247184862922255,
    "25": 0.9981931566365438,
    "50": 0.9999492973895605,
    "75": 0.9999958463029082,
    "95": 0.9999984720917148,
    "100": 0.9999994009639839
  },
  "fungal": {
    "0": 0.8988724357255544,
    "5": 0.9997149465329151,
    "25": 0.9999902497329896,
    "50": 0.9999989548824919,
    "75": 0.9999997681910189,
    "95": 0.9999999614837611,
    "100": 0.9999999938347386
  },
  "leukaemia": {
    "0": 0.996443418893247,
    "5": 0.9993263747948612,
    "25": 0.9997540344535537,
    "50": 0.9999612079661206,
    "75": 0.9999930321658516,
    "95": 0.9999988186767697,
    "100": 0.9999999015711584
  },
  "breast_cancer": {
    "0": 0.7271434438688371,
    "5": 0.9144113068089087,
    "25": 0.999919457857416,
    "50": 0.999999707025368,
    "75": 0.999999997726767,
    "95": 0.9999999999758107,
    "100": 0.999999999992466
  },
  "reject": {
    "0": 0.5376541097472483,
    "5": 0.9882708978860727,
    "25": 0.9998438372725951,
    "50": 0.9999978782088639,
    "75": 0.9999999498872346,
    "95": 0.9999999996621306,
    "100": 1.0
  }
}
```
### Coverage versus accuracy
```json
[
  {
    "probability": 0.0,
    "margin": 0.0,
    "accepted": 636,
    "coverage": 0.5683646112600537,
    "accuracy": 0.9842767295597484,
    "disease_coverage": 0.9952305246422893,
    "reject_false_accept": 0.02040816326530612
  },
  {
    "probability": 0.0,
    "margin": 0.1,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.0,
    "margin": 0.2,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.0,
    "margin": 0.3,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.5,
    "margin": 0.0,
    "accepted": 635,
    "coverage": 0.5674709562109026,
    "accuracy": 0.984251968503937,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.02040816326530612
  },
  {
    "probability": 0.5,
    "margin": 0.1,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.5,
    "margin": 0.2,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.5,
    "margin": 0.3,
    "accepted": 634,
    "coverage": 0.5665773011617515,
    "accuracy": 0.9858044164037855,
    "disease_coverage": 0.9936406995230525,
    "reject_false_accept": 0.018367346938775512
  },
  {
    "probability": 0.7,
    "margin": 0.0,
    "accepted": 632,
    "coverage": 0.5647899910634495,
    "accuracy": 0.9873417721518988,
    "disease_coverage": 0.9920508744038156,
    "reject_false_accept": 0.0163265306122449
  },
  {
    "probability": 0.7,
    "margin": 0.1,
    "accepted": 632,
    "coverage": 0.5647899910634495,
    "accuracy": 0.9873417721518988,
    "disease_coverage": 0.9920508744038156,
    "reject_false_accept": 0.0163265306122449
  },
  {
    "probability": 0.7,
    "margin": 0.2,
    "accepted": 632,
    "coverage": 0.5647899910634495,
    "accuracy": 0.9873417721518988,
    "disease_coverage": 0.9920508744038156,
    "reject_false_accept": 0.0163265306122449
  },
  {
    "probability": 0.7,
    "margin": 0.3,
    "accepted": 632,
    "coverage": 0.5647899910634495,
    "accuracy": 0.9873417721518988,
    "disease_coverage": 0.9920508744038156,
    "reject_false_accept": 0.0163265306122449
  },
  {
    "probability": 0.8,
    "margin": 0.0,
    "accepted": 628,
    "coverage": 0.5612153708668454,
    "accuracy": 0.9904458598726115,
    "disease_coverage": 0.9888712241653418,
    "reject_false_accept": 0.012244897959183673
  },
  {
    "probability": 0.8,
    "margin": 0.1,
    "accepted": 628,
    "coverage": 0.5612153708668454,
    "accuracy": 0.9904458598726115,
    "disease_coverage": 0.9888712241653418,
    "reject_false_accept": 0.012244897959183673
  },
  {
    "probability": 0.8,
    "margin": 0.2,
    "accepted": 628,
    "coverage": 0.5612153708668454,
    "accuracy": 0.9904458598726115,
    "disease_coverage": 0.9888712241653418,
    "reject_false_accept": 0.012244897959183673
  },
  {
    "probability": 0.8,
    "margin": 0.3,
    "accepted": 628,
    "coverage": 0.5612153708668454,
    "accuracy": 0.9904458598726115,
    "disease_coverage": 0.9888712241653418,
    "reject_false_accept": 0.012244897959183673
  },
  {
    "probability": 0.9,
    "margin": 0.0,
    "accepted": 622,
    "coverage": 0.5558534405719392,
    "accuracy": 0.9919614147909968,
    "disease_coverage": 0.9809220985691574,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.9,
    "margin": 0.1,
    "accepted": 622,
    "coverage": 0.5558534405719392,
    "accuracy": 0.9919614147909968,
    "disease_coverage": 0.9809220985691574,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.9,
    "margin": 0.2,
    "accepted": 622,
    "coverage": 0.5558534405719392,
    "accuracy": 0.9919614147909968,
    "disease_coverage": 0.9809220985691574,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.9,
    "margin": 0.3,
    "accepted": 622,
    "coverage": 0.5558534405719392,
    "accuracy": 0.9919614147909968,
    "disease_coverage": 0.9809220985691574,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.95,
    "margin": 0.0,
    "accepted": 609,
    "coverage": 0.5442359249329759,
    "accuracy": 0.9917898193760263,
    "disease_coverage": 0.9602543720190779,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.95,
    "margin": 0.1,
    "accepted": 609,
    "coverage": 0.5442359249329759,
    "accuracy": 0.9917898193760263,
    "disease_coverage": 0.9602543720190779,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.95,
    "margin": 0.2,
    "accepted": 609,
    "coverage": 0.5442359249329759,
    "accuracy": 0.9917898193760263,
    "disease_coverage": 0.9602543720190779,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.95,
    "margin": 0.3,
    "accepted": 609,
    "coverage": 0.5442359249329759,
    "accuracy": 0.9917898193760263,
    "disease_coverage": 0.9602543720190779,
    "reject_false_accept": 0.01020408163265306
  },
  {
    "probability": 0.99,
    "margin": 0.0,
    "accepted": 584,
    "coverage": 0.5218945487042002,
    "accuracy": 0.9948630136986302,
    "disease_coverage": 0.9236883942766295,
    "reject_false_accept": 0.006122448979591836
  },
  {
    "probability": 0.99,
    "margin": 0.1,
    "accepted": 584,
    "coverage": 0.5218945487042002,
    "accuracy": 0.9948630136986302,
    "disease_coverage": 0.9236883942766295,
    "reject_false_accept": 0.006122448979591836
  },
  {
    "probability": 0.99,
    "margin": 0.2,
    "accepted": 584,
    "coverage": 0.5218945487042002,
    "accuracy": 0.9948630136986302,
    "disease_coverage": 0.9236883942766295,
    "reject_false_accept": 0.006122448979591836
  },
  {
    "probability": 0.99,
    "margin": 0.3,
    "accepted": 584,
    "coverage": 0.5218945487042002,
    "accuracy": 0.9948630136986302,
    "disease_coverage": 0.9236883942766295,
    "reject_false_accept": 0.006122448979591836
  }
]
```

## Export parity
```json
{
  "max_abs_difference": {
    "probs": 1.7881393432617188e-07,
    "emb": 1.52587890625e-05,
    "maha_percentile": 8.0108642578125e-05
  },
  "argmax_agreement": 1.0,
  "cases": 27,
  "model_bytes": 45769196,
  "sha256": "bdfc1becd7491ef96e689d412412c676f10b153defbf6e017a1364e78fd3ac0f",
  "tested_batches": [
    1,
    4
  ],
  "providers": [
    "CPUExecutionProvider"
  ],
  "blank_probs": [
    2.21247000808944e-06,
    3.2207092317548813e-06,
    7.539005309808999e-06,
    1.2377811799524352e-05,
    0.9999746084213257
  ],
  "noise_probs": [
    1.218805500968756e-09,
    1.4790513702678254e-08,
    5.405250691481456e-10,
    1.2206841404349689e-07,
    0.9999998807907104
  ]
}
```
