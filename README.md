Sketch
======
AndroidWear間で、画面に描かれた絵や文字を同期するアプリです。

動作手順
--------
1. 端末(handheld)でBluetoothを有効にし、Sketchを起動する
2. 過去に接続をしたことがある端末は一覧に表示されている。この場合、片方でStartServerボタン、もう片方で一覧からやり取りする端末を押下する
3. 一覧上にやり取りする端末が表示されていない場合は、片方でDiscoverableボタン、もう片方でSearchボタンを押下する
4. 3を実施した場合、Discoverableボタンを押した方でStartServerボタンを押下する。もう片方で一覧からやり取りする端末を押下する
5. 両方のAndroidWearでSketchを起動する
6. AndroidWearで絵や文字を描くと、それが同期する


TODO
----
1. Bluetooth処理のService化
2. 端末とWearのSyncをDataItemで行っているが、 MessageApiを使用する(DATA_MANAGE_IDは端末とWearのonDataChangedが両方呼ばれるため)
3. 端末間通信をBluetoothで行っているが、WifiP2Pなど複数台とやりとりできるようにする(選択できるようにする)
