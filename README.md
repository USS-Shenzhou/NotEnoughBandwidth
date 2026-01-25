# 网络包优化 | Not Enough Bandwidth (NEB)

## 主要功能 | Main Features

1. 优化`CustomPacketPayload`编码及对应解码，以紧凑的索引替代包头的`Identifier`(Packet Type)，使模组网络包包头消耗减少为固定3-4字节，而不是网络包类型对应的字符串长度。


1. Optimize `CustomPacketPayload` encoding and decoding by replacing the packet header `Identifier` (Packet Type) with a compact index. This reduces the mod network packet header overhead to a fixed 3-4 bytes, instead of the length of the string corresponding to the network packet type.

索引结构如下：

The index structure is as follows:

> [!NOTE]
> ### Fixed 8 bits header
> ```
> ┌------------- 1 byte (8 bits) ---------------┐
> │               function flags                │
> ├---┬---┬-------------------------------------┤
> │ i │ t │      reserved (6 bits)              │
> └---┴---┴-------------------------------------┘
> ```
> - i = indexed (1 bit)
> - t = tight_indexed (1 bit, only valid if i=1)
> - reserved = 6 bits (for future use)
>
> ### Indexed packet type
> - If i=0 (not indexed):
> ```
> ┌---------------- N bytes ----------------
> │ Identifier (packet type) in UTF-8
> └-----------------------------------------
> ```
> - If i=1 and t=0 (indexed, NOT tight):
> ```
> ┌-------- 1 byte ---------┬-------- 1 byte --------┬-------- 1 byte --------┐
> ┌------------- 12 bits ---------------┬-------------- 12 bits --------------┐
> │    namespace-id (capacity 4096)     │       path-id (capacity 4096)       │
> └-------------------------------------┴-------------------------------------┘
> ```
> - If i=1 and t=1 (indexed, tight):
> ```
> ┌--------- 1 byte ----------┬--------- 1 byte ---------┐
> ┌--------- 8 bits ----------┬--------- 8 bits ---------┐
> │namespace-id (capacity 256)│  path-id (capacity 256)  │
> └---------------------------┴--------------------------┘
> ```
> Then packet data.

网络包`namespace`及对应的每个`path`少于256时为3字节，大于256时为4字节。即最多支持4096个模组，每个模组4096条通道。

It occupies 3 bytes when the network packet namespace and its corresponding path are fewer than 256, and 4 bytes when greater than 256. This supports up to 4096 mods, with 4096 channels per mod.

2. 优化原版经常出现大量小体积网络包的情况，在`Connection`层面拦截发送，每隔20ms组装为一个大网络包，并进行压缩后发送。


2. Optimize the situation where vanilla often produces a large number of small network packets. Intercept transmission at the `Connection` level, assemble them into one large network packet every 20ms, and send it after compression.

> [!NOTE]
> ```
> ┌---┬----┬----┬----┬----┬----┬----...
> │ S │ p0 │ s0 │ d0 │ p1 │ s1 │ d1 ...
> └---┴----┴----┴----┴----┴----┴----...
>     └--packet 1---┘└--packet 2---┘
>     └----------compressed----------┘
> ```
> - S = varint, size of compressed buf
> - p = prefix (medium/int/utf-8)， type of this subpacket
> - s = varint, size of this subpacket
> - d = bytes, data of this subpacket

## 配置 | Config

在`config/NotEnoughBandwidthConfig.json`修改配置文件。

Modify the configuration file at `config/NotEnoughBandwidthConfig.json`.

### compatibleMode

是否要开启兼容模式。如果为true，下方的`blackList`会被启用。

Whether to enable compatibility mode. If set to `true`, the `blackList` below will be used.

### blackList

兼容模式黑名单。在黑名单中的包会被NEB跳过。默认自带一系列和velocity相关的包，你也可以按需增加新的包。

The blacklist for compatibility mode. Packets listed here will be skipped by NEB. By default, it includes a list of Velocity-related packets, but you can add new packets as needed.

### contextLevel

在进行压缩时的上下文窗口长度。可选范围为21\~25的整数，分别代表2\~32MB。默认为23，即8MB。

上下文窗口长度越长，则压缩效果越好，越节省流量；但也会消耗更多的内存。对于100名玩家的服务器，设置为25会产生约额外3200的内存占用。

The context window size used for compression. Valid values are integers from 21 to 25, representing 2MB to 32MB respectively. The default is 23 (8MB).

A larger context window results in better compression and bandwidth savings, but consumes more memory. For a server with 100 players, a setting of 25 will result in approximately 3200MB of additional memory usage.

## 版权和许可 | Copyrights and Licenses

Copyright (C) 2025 USS_Shenzhou

本模组是自由软件，你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。

发布这个模组是希望它能有用，但是并无保障；甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。

Copyright (C) 2025 USS_Shenzhou

This mod is free software; you can redistribute it and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

### 额外许可 | Additional Permissions

a）当你作为游戏玩家，加载本程序于Minecraft并游玩时，本许可证自动地授予你一切为正常加载本程序于Minecraft并游玩所必要的、不在GPL-3.0许可证内容中、或是GPL-3.0许可证所不允许的权利。如果GPL-3.0许可证内容与Minecraft EULA或其他Mojang/微软条款产生冲突，以后者为准。

a) As a game player, when you load and play this program in Minecraft, this license automatically grants you all rights necessary, which are not covered in the GPL-3.0 license, or are prohibited by the GPL-3.0 license, for the normal loading and playing of this program in Minecraft. In case of conflicts between the GPL-3.0 license and the Minecraft EULA or other Mojang/Microsoft terms, the latter shall prevail.
