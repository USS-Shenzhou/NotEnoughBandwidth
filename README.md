# 网络包优化 | Not Enough Bandwidth

## 功能 | Features

1. 优化`CustomPacketPayload`编码及对应解码，以索引替代`Identifier`(Packet Type)，使模组网络包包头消耗减少为固定3-4字节（网络包`namespace`及对应的每个`path`少于256时为3字节，大于256时为4字节。即最多支持4096个模组，每个模组4096条通道。）。
2. 优化原版经常出现大量小体积网络包的情况，在`Connection`层面拦截发送，每隔固定间隔组装为一个大网络包并进行压缩后发送。
3. 


1. Optimizes the encoding and decoding of `CustomPacketPayload` by using an index instead of `Identifier` (Packet Type). This reduces the mod network packet header size to a fixed 3–4 bytes (3 bytes when network namespaces and each one's paths are less than 256, otherwise 4 bytes, supporting up to 4096 mods and 4096 paths for each mod).
2. WIP: Optimizes vanilla block entity data.
3. WIP: Optimizes vanilla level_chunk_with_light.

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
