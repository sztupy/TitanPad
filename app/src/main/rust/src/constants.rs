// add device 1: event0
//   bus:      0019
//   vendor    0001
//   product   0001
//   version   0100
//   name:     "gpio-keys"
//   location: "gpio-keys/input0"
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0072  0073
//   input props:
//     <none>
// Volume Up & Down

// add device 1: event1
//   bus:      0019
//   vendor    0001
//   product   0001
//   version   0001
//   name:     "mtk-pmic-keys"
//   location: ""
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0074  00f9  00fa
//   input props:
//     <none>
// Func 1 (00f9) & Power

// add device 1: event2
//   bus:      0019
//   vendor    00fa
//   product   00fa
//   version   0100
//   name:     "gpio_key-func"
//   location: ""
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 00fa
//   input props:
//     <none>
// Func 2 (00fa)

// add device 1: event3
//   bus:      0019
//   vendor    0000
//   product   0000
//   version   0000
//   name:     "mt6878-mt6369 Headset Jack"
//   location: "ALSA"
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0072  0073  00a4  0246
//     SW  (0005): 0002  0004  0006  0007
//   input props:
//     <none>
// event3: Headset Jack (?)

// add device 1: event4
//   bus:      0018
//   vendor    0000
//   product   0000
//   version   0000
//   name:     "sub_touch"
//   location: ""
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0011  0012  0016  0018  001f  0026  002c  002e
//                 002f  0032  0040  0067  0069  006a  006c  0074
//                 0145  014a  0179
//     ABS (0003): 002f  : value 0, min 0, max 2, fuzz 0, flat 0, resolution 0
//                 0030  : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
//                 0032  : value 0, min 0, max 200, fuzz 0, flat 0, resolution 0
//                 0035  : value 0, min 0, max 410, fuzz 0, flat 0, resolution 0
//                 0036  : value 0, min 0, max 502, fuzz 0, flat 0, resolution 0
//                 0039  : value 0, min 0, max 2, fuzz 0, flat 0, resolution 0
//                 003a  : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
//   input props:
//     INPUT_PROP_DIRECT
// Subscreen Touch

// add device 1: event5
//   bus:      0018
//   vendor    0000
//   product   0003
//   version   2000
//   name:     "synaptics_dsx_i2c"
//   location: "synaptics_dsx_i2c/input0"
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0074  009e  00ac  0145  014a  0244
//     ABS (0003): 002f  : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
//                 0030  : value 0, min 0, max 20, fuzz 0, flat 0, resolution 0
//                 0031  : value 0, min 0, max 20, fuzz 0, flat 0, resolution 0
//                 0035  : value 0, min 0, max 1439, fuzz 0, flat 0, resolution 0
//                 0036  : value 0, min 0, max 1439, fuzz 0, flat 0, resolution 0
//                 0039  : value 0, min 0, max 65535, fuzz 0, flat 0, resolution 0
//   input props:
//     INPUT_PROP_DIRECT
// Main Touch

// add device 1: event6
//   bus:      0000
//   vendor    2533
//   product   2533
//   version   0000
//   name:     "TitanKey"
//   location: ""
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 000e  0010  0011  0012  0013  0014  0015  0016
//                 0017  0018  0019  001c  001e  001f  0020  0021
//                 0022  0023  0024  0025  0026  002a  002c  002d
//                 002e  002f  0030  0031  0032  0034  0039  003b
//                 0044  0057  0064  0067  0069  006a  006c  0073
//                 0074  0080  0081  0083  0084  0085  0086  0087
//                 0088  0089  009e  00b7  00de  00fb  00fd  0244
//   input props:
//     <none>
// Keyboard
// Sym: 00fd
// Fn: 00fb

// add device 1: event7
//   bus:      0018
//   vendor    0000
//   product   0000
//   version   0000
//   name:     "touchPad"
//   location: "touchPad/input0"
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 0145  014a
//     ABS (0003): 0000  : value 0, min 0, max 0, fuzz 0, flat 0, resolution 0
//                 0001  : value 0, min 0, max 0, fuzz 0, flat 0, resolution 0
//                 0018  : value 0, min 0, max 0, fuzz 0, flat 0, resolution 0
//                 0030  : value 0, min 0, max 8, fuzz 0, flat 0, resolution 0
//                 0031  : value 0, min 0, max 8, fuzz 0, flat 0, resolution 0
//                 0035  : value 0, min 0, max 1440, fuzz 0, flat 0, resolution 0
//                 0036  : value 0, min 0, max 720, fuzz 0, flat 0, resolution 0
//   input props:
//     INPUT_PROP_DIRECT
// Touchpad

// add device 1: event8
//   bus:      0000
//   vendor    0000
//   product   0000
//   version   0000
//   name:     "ff_key"
//   location: ""
//   id:       ""
//   version:  1.0.1
//   events:
//     KEY (0001): 001c  0067  0069  006a  006c  0074  009e  00f9
//   input props:
//     <none>
// Unknown purpose; has some of the keycodes as above but looks to be unused under normal circumstances
