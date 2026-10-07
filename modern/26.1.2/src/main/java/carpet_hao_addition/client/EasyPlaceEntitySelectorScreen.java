package carpet_hao_addition.client;

import carpet_hao_addition.EasyPlaceEntityHandler;
import carpet_hao_addition.EasyPlaceEntityPayload;
import carpet_hao_addition.EasyPlaceEntitySettings;
import carpet_hao_addition.EntitySpawn;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 昊藏小囊(轻松放置实体)的候选选择器 —— 与 1.21.8 的实现逐条对齐。
 * <p>
 * 视线射线同时命中多个投影实体时(叠在一起的矿车、上面的箱船等),由玩家自己点一格决定放哪个 ——
 * 不再由代码猜"你指着谁"。
 * <p>
 * 面板用原版箱子界面贴图拼(材质包替换即适配);因为材质包可能把贴图放大(CozyUI 是 4×),
 * 先按贴图实际尺寸算好"贴图坐标系下的绘制尺寸",再用矩阵缩回屏幕尺寸,uv 与显示尺寸才都对。
 * <p>
 * 与 1.21.8 的差异只在 API 名:绘制走 {@link GuiGraphicsExtractor}(26.x 的"渲染状态提取"模型),
 * 覆写入口是 {@code extractRenderState} 而不是 {@code render};矩阵入口是 {@code pose()};
 * 鼠标事件是 {@code mouseClicked(MouseButtonEvent, boolean)};Yarn 名换成 Mojang 名。
 */
public class EasyPlaceEntitySelectorScreen extends Screen {
	private static final int COLUMNS = 9;
	private static final int ROWS_MAX = 6;
	/**
	 * 最少显示 3 行。
	 * <p>
	 * 只有一个候选时如果只画一行,面板会扁成一条,看着很突兀、比例也不对称;
	 * 固定成原版箱子最小行数(3 行)后就长得像个正经界面了。
	 */
	private static final int ROWS_MIN = 3;
	private static final int SLOT_SIZE = 18;
	private static final int PANEL_WIDTH = 176;
	private static final Identifier LOGO =
			Identifier.fromNamespaceAndPath("carpet-hao-addition", "textures/gui/logo.png");
	private static final Identifier PANEL_TEXTURE =
			Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	/** 原版箱子界面的比例:标题栏 17 高,之后每行 18。 */
	private static final int PANEL_HEADER = 17;
	/** 底部栏高度 —— 取标题栏那段素材,旋转 180° 装到下面(上下对称)。 */
	private static final int BOTTOM_BAR = 14;
	/** 信息条高度(画在**面板上方**)。 */
	private static final int INFO_HEIGHT = 14;
	/** 信息条与面板之间的留白 —— 这里也用来放"挂绳"。 */
	private static final int INFO_GAP = 8;
	/** 槽位区相对面板左上角的偏移。 */
	private static final int GRID_LEFT = 8;
	private static final int GRID_TOP = 18;

	// —— 深色卡片风配色(不依赖材质包,自带层次)——
	private static final int COLOR_HOVER = 0xFF8C8CE8;       // 悬停/高亮(柔和紫)
	private static final int COLOR_PLACED = 0xFF6AA84F;      // 已发出(绿)
	private static final int COLOR_MISSING = 0xFFE06060;     // 材料不足:红色外框/红字

	private final List<EntitySpawn> candidates;
	private final List<ItemStack> icons = new ArrayList<>();
	private final List<Component> names = new ArrayList<>();
	private final int rows;
	private final int panelHeight;
	/** 悬停项的所需材料(按当前 count 算一次后缓存)。 */
	private int materialCacheIndex = -1;
	private Map<Item, Integer> materialCache;
	/** 材料不足时的提示(显示若干 tick)。 */
	private String notice;
	private int noticeTicks;
	/** 已经点过(已发给服务端)的候选下标 —— 标绿、不可重复点,但不关闭界面。 */
	private final Set<Integer> placed = new LinkedHashSet<>();

	public EasyPlaceEntitySelectorScreen(List<EntitySpawn> candidates) {
		super(Component.literal("昊藏小囊"));
		this.candidates = candidates;
		this.rows = Math.max(ROWS_MIN,
				Math.min(ROWS_MAX, (candidates.size() + COLUMNS - 1) / COLUMNS));
		// 面板 = 顶部标题栏 + 实体格子 + 底部栏(与标题栏等高,上下对称)
		this.panelHeight = PANEL_HEADER + this.rows * SLOT_SIZE + BOTTOM_BAR;
		for (EntitySpawn spawn : candidates) {
			this.icons.add(EasyPlaceEntityMaterials.iconFor(spawn.type(), spawn.nbt()));
			this.names.add(nameOf(spawn.type()));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.noticeTicks > 0) {
			this.noticeTicks--;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = panelTop();
		int right = left + PANEL_WIDTH;
		int bottom = top + this.panelHeight;

		// 信息条的位置(只画文字,透明底);与面板之间留 INFO_GAP,免得挤在一起
		int infoTop = top - INFO_HEIGHT - INFO_GAP;

		// —— 面板:投影 + 原版容器贴图(材质包替换即适配)——
		// 坑:blit 的 w/h **同时**被当成"屏幕尺寸"和"uv 尺寸",uv2 = (v+h)/texH。
		// 材质包把贴图放大了(CozyUI 是 4×),直接传屏幕尺寸会让 uv 只取到贴图的一小块
		// (画出来就是一整块纯灰)。所以:先按放大倍数 k 算好"贴图坐标系下的绘制尺寸",
		// 再用矩阵整体缩回屏幕尺寸 —— uv 和显示尺寸就都对了。
		int[] textureSize = textureSize();
		int texW = textureSize[0];
		int texH = textureSize[1];
		float k = texH / 256.0F;
		int drawW = Math.round(PANEL_WIDTH * k);
		int drawUpperH = Math.round((PANEL_HEADER + this.rows * SLOT_SIZE) * k);
		int drawBottom = drawUpperH + Math.round(BOTTOM_BAR * k);
		var matrices = extractor.pose();
		matrices.pushMatrix();
		matrices.translate(left, top);
		matrices.scale(1.0F / k, 1.0F / k);
		// 上段:标题栏 + 实体格子
		extractor.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, 0, 0, 0.0F, 0.0F,
				drawW, drawUpperH, texW, texH);
		// 信息条:做成一个**小面板** —— 底色、左右外框都取自贴图,四角也是圆的,
		// 与下面的大面板同源;上半圆角朝上、下半翻转过来圆角朝下,各取贴图前 8 行。
		float infoTopY = -(INFO_HEIGHT + INFO_GAP) * k;
		int infoHalf = Math.max(1, INFO_HEIGHT / 2);
		matrices.pushMatrix();
		matrices.translate(0.0F, infoTopY);
		matrices.scale(1.0F, (float) infoHalf / 8.0F);
		extractor.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, 0, 0, 0.0F, 0.0F, drawW,
				Math.round(8 * k), texW, texH);
		matrices.popMatrix();
		matrices.pushMatrix();
		// 下半:锚点必须取信息条的**底边**(负缩放是往上生长的),否则会和上半重叠、下面留缝。
		matrices.translate(drawW, infoTopY + Math.round(INFO_HEIGHT * k));
		matrices.scale(-1.0F, -(float) infoHalf / 8.0F);
		extractor.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, 0, 0, 0.0F, 0.0F, drawW,
				Math.round(8 * k), texW, texH);
		matrices.popMatrix();
		// 底栏:整段取标题栏那 17 行,并**以右下角为原点旋转 180°**装到下面,再纵向压到
		// BOTTOM_BAR 的高度(比标题栏略矮)。这样四个角的圆弧、左右边框和上面完全对称。
		float barSquash = (float) BOTTOM_BAR / PANEL_HEADER;
		matrices.pushMatrix();
		matrices.translate(drawW, drawBottom);
		matrices.scale(-1.0F, -1.0F * barSquash);
		extractor.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, 0, 0, 0.0F, 0.0F, drawW,
				Math.round(PANEL_HEADER * k), texW, texH);
		matrices.popMatrix();
		matrices.popMatrix();
		// 挂绳:两根简单的灰色小杆(不分节),与面板底色搭配
		int ropeWidth = 2;
		int ropeTop = infoTop + INFO_HEIGHT;
		for (int side = 0; side < 2; side++) {
			int ropeX = side == 0 ? left + 46 : right - 46 - ropeWidth;
			extractor.fill(ropeX - 1, ropeTop, ropeX + ropeWidth + 1, top, 0xFF161616);   // 深色描边
			extractor.fill(ropeX, ropeTop, ropeX + ropeWidth, top, 0xFF5A5A5A);           // 灰主体
			extractor.fill(ropeX, ropeTop, ropeX + 1, top, 0xFF9A9A9A);                   // 亮面高光
		}

		// 标题栏左侧的小图标(按原图 1280:750 的比例,高 10px)
		int logoHeight = 10;
		int logoWidth = Math.max(1, Math.round(logoHeight * 1280.0F / 750.0F));
		extractor.blit(RenderPipelines.GUI_TEXTURED, LOGO, left + 4, top + 4, 0.0F, 0.0F,
				logoWidth, logoHeight, logoWidth, logoHeight);
		// 标题用带阴影的白字:材质包的标题栏深浅不定,这样都读得清
		centeredShadow(extractor, this.title, this.width / 2, top + 5, 0xFFFFFFFF);
		// 底部署名:字体与颜色都和标题一致(同一个 font、同样的纯白)
		centeredShadow(extractor, Component.literal("Carpet-Hao-Addition"), this.width / 2,
				top + PANEL_HEADER + this.rows * SLOT_SIZE + 3, 0xFFFFFFFF);

		int hovered = hoveredIndex(mouseX, mouseY, left, top);
		for (int index = 0; index < Math.min(this.candidates.size(), COLUMNS * this.rows); index++) {
			int slotX = left + GRID_LEFT + (index % COLUMNS) * SLOT_SIZE;
			int slotY = top + GRID_TOP + (index / COLUMNS) * SLOT_SIZE;
			boolean isHovered = index == hovered;
			// 红框只看**实体本身**够不够:内容物/乘客是"有就带、没有就不带",不该挡着不放
			boolean affordable = enough(baseCost(index));
			// 格子底由贴图提供,这里只叠加状态
			if (this.placed.contains(index)) {
				extractor.fill(slotX, slotY, slotX + 16, slotY + 16, 0xB06AA84F); // 已发出
			} else if (isHovered) {
				extractor.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFFFFF); // 悬停微亮
			}
			// 描边:材料不足 = 红,悬停 = 紫
			int frame = !affordable ? COLOR_MISSING : (isHovered ? COLOR_HOVER : 0);
			if (frame != 0) {
				extractor.fill(slotX - 1, slotY - 1, slotX + 17, slotY, frame);
				extractor.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, frame);
				extractor.fill(slotX - 1, slotY, slotX, slotY + 16, frame);
				extractor.fill(slotX + 16, slotY, slotX + 17, slotY + 16, frame);
			}
			extractor.item(this.icons.get(index), slotX, slotY);
		}

		// 信息文字画在**上方**那条里
		int infoY = infoTop + 3;
		if (hovered >= 0) {
			centeredShadow(extractor, this.names.get(hovered), this.width / 2, infoY, 0xFFF0F0F0);
			renderMaterials(extractor, hovered, mouseX, mouseY);
		} else if (this.noticeTicks > 0 && this.notice != null) {
			centeredShadow(extractor, Component.literal(this.notice), this.width / 2, infoY, 0xFFFF8080);
		} else {
			centeredShadow(extractor, Component.literal("点击要放的那一个 · 还剩 "
							+ (this.candidates.size() - this.placed.size()) + " 个"),
					this.width / 2, infoY, 0xFFD8D8D8);
		}
	}

	/** 面板贴图的实际宽高缓存(材质包可能把 256×256 放大)。 */
	private static int[] texSize;

	/**
	 * 面板贴图的实际宽高,用于 uv 归一化。
	 * <p>
	 * 直接读 PNG 头(偏移 16 = 宽,20 = 高),不依赖任何可能变动的纹理 API。
	 * 宽高必须一起换算:只换高度、宽写死 256 会让面板被拉伸变形。
	 */
	private static int[] textureSize() {
		if (texSize != null) {
			return texSize;
		}
		int width = 256;
		int height = 256;
		try (var stream = Minecraft.getInstance().getResourceManager()
				.getResource(PANEL_TEXTURE).orElseThrow().open()) {
			byte[] header = stream.readNBytes(24);
			if (header.length >= 24) {
				width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
						| ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
				height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
						| ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
			}
		} catch (Throwable throwable) {
			width = 256; // 读不到就按原版尺寸
			height = 256;
		}
		if (width <= 0) {
			width = 256;
		}
		if (height <= 0) {
			height = 256;
		}
		texSize = new int[] {width, height};
		return texSize;
	}

	/** 居中画一行文字,带原版阴影(用于深浅不定的材质包下都读得清)。 */
	private void centeredShadow(GuiGraphicsExtractor extractor, Component text, int centerX, int y, int color) {
		extractor.text(this.font, text, centerX - this.font.width(text) / 2, y, color, true);
	}

	/** 这个候选**实体本身**需要的材料(不含内容物/乘客 —— 那些是"有就带"的可选项)。 */
	private Map<Item, Integer> baseCost(int index) {
		EntitySpawn spawn = this.candidates.get(index);
		return EasyPlaceEntityCosts.baseCostFor(entityType(spawn), currentCount());
	}

	/** 悬停项的所需材料(缓存,避免每帧重算)。 */
	private Map<Item, Integer> materials(int index) {
		if (this.materialCache == null || this.materialCacheIndex != index) {
			EntitySpawn spawn = this.candidates.get(index);
			this.materialCache = EasyPlaceEntityCosts.materialsFor(entityType(spawn), spawn.nbt(),
					currentCount());
			this.materialCacheIndex = index;
		}
		return this.materialCache;
	}

	/**
	 * 在鼠标旁逐行列出这个实体要花什么(实体本身 + 内容物 + 乘客)。
	 * <p>
	 * 例:竹筏上驮着雪傀儡 → 竹筏 ×1 / 雪块 ×2 / 雕刻南瓜 ×1;
	 * 漏斗矿车里装着骨粉 → 漏斗矿车 ×1 / 骨粉 ×5组。
	 */
	private void renderMaterials(GuiGraphicsExtractor extractor, int index, int mouseX, int mouseY) {
		Map<Item, Integer> materials = materials(index);
		if (materials.isEmpty()) {
			return;
		}
		List<Map.Entry<Item, Integer>> entries = new ArrayList<>(materials.entrySet());
		int width = this.font.width("所需材料");
		for (Map.Entry<Item, Integer> entry : entries) {
			int line = 22 + this.font.width(new ItemStack(entry.getKey()).getHoverName().getString())
					+ this.font.width(amount(entry.getValue()));
			width = Math.max(width, line);
		}
		width += 8;
		int height = 13 + entries.size() * 18 + 3;
		int x = mouseX + 12;
		if (x + width > this.width - 4) {
			x = mouseX - width - 12;
		}
		int y = Math.max(4, Math.min(mouseY - 4, this.height - height - 4));
		// 深色小面板,保证在浅色大面板上也看得清
		extractor.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
		extractor.fill(x, y, x + width, y + height, 0xF01E1E26);
		extractor.text(this.font, Component.literal("所需材料"), x + 4, y + 3, 0xFFE6E6A0, false);
		int lineY = y + 13;
		for (Map.Entry<Item, Integer> entry : entries) {
			boolean enough = owned(entry.getKey()) >= entry.getValue();
			extractor.item(new ItemStack(entry.getKey()), x + 4, lineY);
			extractor.text(this.font, new ItemStack(entry.getKey()).getHoverName(), x + 22, lineY + 5,
					enough ? 0xFFF0F0F0 : 0xFFFF9090, false);
			Component amount = amount(entry.getValue());
			extractor.text(this.font, amount,
					x + width - 4 - this.font.width(amount), lineY + 5,
					enough ? 0xFFFFD060 : 0xFFFF7070, false);
			lineY += 18;
		}
	}

	/** 背包里该物品的总数(创造模式视为无限)。 */
	private int owned(Item item) {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return 0;
		}
		if (player.isCreative()) {
			return Integer.MAX_VALUE;
		}
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** 清单里的东西是否都够。 */
	private boolean enough(Map<Item, Integer> materials) {
		for (Map.Entry<Item, Integer> entry : materials.entrySet()) {
			if (owned(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	/** 数量文案:超过一组就用"n组"表示,更贴近实际备料习惯。 */
	private static Component amount(int amount) {
		if (amount >= 64) {
			int stacks = amount / 64;
			int rest = amount % 64;
			return rest == 0 ? Component.literal("×" + stacks + "组")
					: Component.literal("×" + amount + "(" + stacks + "组+" + rest + ")");
		}
		return Component.literal("×" + amount);
	}

	/** 面板(纹理部分)左上角 y:整体垂直居中,并给上方的信息条留出位置。 */
	private int panelTop() {
		return (this.height - (INFO_HEIGHT + INFO_GAP + this.panelHeight)) / 2 + INFO_HEIGHT + INFO_GAP;
	}

	/** 命中的槽位下标;-1 表示没指在有效格子上。 */
	private int hoveredIndex(int mouseX, int mouseY, int left, int top) {
		if (mouseX < left + GRID_LEFT || mouseY < top + GRID_TOP) {
			return -1;
		}
		int column = (mouseX - left - GRID_LEFT) / SLOT_SIZE;
		int row = (mouseY - top - GRID_TOP) / SLOT_SIZE;
		if (column < 0 || column >= COLUMNS || row < 0 || row >= this.rows) {
			return -1;
		}
		int slotX = left + GRID_LEFT + column * SLOT_SIZE;
		int slotY = top + GRID_TOP + row * SLOT_SIZE;
		if (mouseX >= slotX + 16 || mouseY >= slotY + 16) {
			return -1; // 槽位之间的 2px 空隙
		}
		int index = row * COLUMNS + column;
		return index < this.candidates.size() ? index : -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int index = hoveredIndex((int) event.x(), (int) event.y(), left, panelTop());
		if (index < 0 || this.placed.contains(index)) {
			return super.mouseClicked(event, doubled);
		}
		// 只有**实体本身**放不出来时才拦:内容物/乘客是"有就带、没有就不带"的附加项,
		// 投影里漏斗矿车装着 5 组骨粉而你没骨粉时,漏斗矿车本身照样应该放出来。
		if (!enough(baseCost(index))) {
			this.notice = "材料不足:这个实体本身放不出来";
			this.noticeTicks = 60;
			return true;
		}
		place(this.candidates.get(index));
		this.placed.add(index);
		// 关键:点完**不关界面** —— 投影里有两辆矿车这类情况,可以连着把剩下的点完。
		// 只有全部都点过了才自动收起来。
		if (this.placed.size() >= this.candidates.size()) {
			this.onClose();
		}
		return true;
	}

	/** 把选中的那一个发给服务端(沿用既有 payload,协议字段未变)。 */
	private void place(EntitySpawn spawn) {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int count = EasyPlaceEntitySettings.count(player.getUUID());
		EasyPlaceEntityHandler.registerPayloadType();
		ClientPlayNetworking.send(new EasyPlaceEntityPayload(List.of(spawn), count));
		System.out.println("[hao-entity] 选择器放置 " + spawn.type() + " @ "
				+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z()));
	}

	private int currentCount() {
		var player = Minecraft.getInstance().player;
		return player == null ? EasyPlaceEntitySettings.DEFAULT_COUNT
				: EasyPlaceEntitySettings.count(player.getUUID());
	}

	private static EntityType<?> entityType(EntitySpawn spawn) {
		return BuiltInRegistries.ENTITY_TYPE.getValue(spawn.type());
	}

	/** 实体的显示名(跟随语言文件,中文客户端就是中文名)。 */
	private static Component nameOf(Identifier entityId) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
		return type == null ? Component.literal(entityId.toString()) : type.getDescription();
	}
}
