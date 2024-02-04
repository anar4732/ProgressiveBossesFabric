package insane96mcp.progressivebosses.capability;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import insane96mcp.progressivebosses.utils.Strings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class Difficulty implements IDifficulty, AutoSyncedComponent {
	public static final Identifier IDENTIFIER = new Identifier(Strings.Tags.DIFFICULTY);

	private int spawnedWithers;
	private int killedDragons;
	// 0 = just spawned, 1 = first dragon, 2 = first dragon spawned
	private byte firstDragon;

	@Override
	public int getSpawnedWithers() {
		return this.spawnedWithers;
	}

	@Override
	public void setSpawnedWithers(int spawnedWithers) {
		this.spawnedWithers = MathHelper.clamp(spawnedWithers, 0, Integer.MAX_VALUE);
	}

	@Override
	public int getKilledDragons() {
		return this.killedDragons;
	}

	@Override
	public void setKilledDragons(int killedDragons) {
		this.killedDragons = MathHelper.clamp(killedDragons, 0, Integer.MAX_VALUE);
	}

	@Override
	public byte getFirstDragon() {
		return this.firstDragon;
	}

	@Override
	public void setFirstDragon(byte firstDragon) {
		this.firstDragon = firstDragon;
	}


	@Override
	public void addSpawnedWithers(int amount) {
		this.setSpawnedWithers(this.getSpawnedWithers() + amount);
	}

	@Override
	public void addKilledDragons(int amount) {
		this.setKilledDragons(this.getKilledDragons() + amount);
	}

	@Override
	public void readFromNbt(NbtCompound nbt) {
		this.setSpawnedWithers(nbt.getInt(Strings.Tags.SPAWNED_WITHERS));
		this.setKilledDragons(nbt.getInt(Strings.Tags.KILLED_DRAGONS));
		this.setFirstDragon(nbt.getByte(Strings.Tags.FIRST_DRAGON));
	}

	@Override
	public void writeToNbt(NbtCompound nbt) {
		nbt.putInt(Strings.Tags.SPAWNED_WITHERS, this.getSpawnedWithers());
		nbt.putInt(Strings.Tags.KILLED_DRAGONS, this.getKilledDragons());
		nbt.putByte(Strings.Tags.FIRST_DRAGON, this.getFirstDragon());
	}

}