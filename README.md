# BluetoothTemperatureControl
连接制定物理地址的蓝牙设备进行数据的传输和显示

**一、**     **示意图**

![图1 软件示意图](file:///C:/Users/我是刘~1/AppData/Local/Temp/msohtmlclip1/01/clip_image001.png)

​																				图1 软件示意图

**二、**     **操作步骤**

首次运行软件时，将动态获取蓝牙，位置等权限，请同意。

软件运行后，自动连接地址号为：34:23:87:40:73:04 的蓝牙，如需连接指定蓝牙地址，请在如下图中标识为1 的地址栏输入制定蓝牙地址（注意，蓝牙地址格式为两数字后加冒号的方式）；输入完成17位蓝牙地址后，点击右侧【连接】按钮即可。

说明：仅可连接一个蓝牙地址。点击连接后，按钮将显示“连接中”，此时请等待软件断开上一个蓝牙连接请求，连接本次蓝牙，这可能需要一些时间；连接成功后，按钮将重新显示为“连接”，手机底部将弹出时长为1 秒钟“蓝牙连接成功”的Toast提示。

蓝牙连接过程中，也可以进行连接其他蓝牙地址的操作，此时将断开上一次未成功的蓝牙连接请求，开始新的连接。

蓝牙连接成功后，将保存该蓝牙地址，再次进入软件时，将自动连接该地址。



![img](file:///C:/Users/我是刘~1/AppData/Local/Temp/msohtmlclip1/01/clip_image002.png) 

​																图2 连接蓝牙步骤示意图

**三、**     **关于折线图**

软件默认的温度区间为 0 – 100；温度间隔为10，根据实际需要进行调整时，请点击右上角【齿轮】按钮，此时在页面上部弹出如下图所示列表；根据提示，结合需求填入即可，填写完成后点击【确认】按钮，即可完成设置。

注意，在设置完成折线图区间后，请重启软件，否则显示结果不美观。

显示的当前温度将用蓝色的曲线线条表示，当前设定温度由灰色曲线线条显示，蓝牙模块发送来的功率数据，将显示在表格下方的功率对话框。

![img](file:///C:/Users/我是刘~1/AppData/Local/Temp/msohtmlclip1/01/clip_image003.png)

图3 设定温度范围示意图

**四、**     **关于信息传递**

信息传递格式完全按照串口命令进行，操作简单。

如下图所示，功率对话框将显示蓝牙模块发送来的当前功率数据；

如需进行调试，在发送命令前方的黄色文本框内填写内容点击【发送命令】即可。

下方分别是蓝牙连接模块和数据操作、显示模块；每一个数据对应一个小的文本框，每个小的文本框仅可以输入一个数字，如需设定正温度，请在前方文本框内输入对应数据后，点击右侧按钮【设定正温度】即可，设定负温度同理；注意：如设定正温度12.3，请输入123；

点击【重置PID】，将发送重置PID参数命令；

点击【获取PID】，将发送获取PID请求，并将蓝牙模块的返回值，依次填写在P、I、D参数前的文本框内；

点击【获取偏置量】，将发送获取偏置量请求，并将蓝牙模块的返回值，填写在偏置量参数前的文本框内；[注意：此处由于（串口命令中没有对政府进行严格区分）蓝牙模块的返回值中没有数据正负的区分和判断，所以此处填写的数据是传递过来的数据，没有符号]；

点击【获取温度】将发送获取环境温度的请求，并将蓝牙模块的返回值，填写在当前环境温度参数前的文本框内；[注意：此处由于（串口命令中没有对政府进行严格区分）蓝牙模块的返回值中没有数据正负的区分和判断，所以此处填写的数据是传递过来的数据，没有符号]

 

![img](file:///C:/Users/我是刘~1/AppData/Local/Temp/msohtmlclip1/01/clip_image004.png)

​																			图4 命令发送示意图