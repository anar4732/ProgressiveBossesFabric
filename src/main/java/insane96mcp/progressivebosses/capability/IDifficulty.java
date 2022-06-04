package insane96mcp.progressivebosses.capability;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;

public interface IDifficulty extends ComponentV3 {
	int getSpawnedWithers();
	void setSpawnedWithers(int spawnedWithers);
	int getKilledDragons();
	void setKilledDragons(int killedDragons);
	byte getFirstDragon();
	void setFirstDragon(byte firstDragon);
	void addSpawnedWithers(int amount);
	void addKilledDragons(int amount);
}