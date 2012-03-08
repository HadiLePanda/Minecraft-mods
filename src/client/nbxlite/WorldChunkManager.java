package net.minecraft.src;

import java.util.*;
import net.minecraft.src.nbxlite.noise.NoiseGeneratorOctaves2;
import net.minecraft.src.nbxlite.oldbiomes.*;

public class WorldChunkManager
{
    private GenLayer genBiomes;

    /** A GenLayer containing the indices into BiomeGenBase.biomeList[] */
    private GenLayer biomeIndexLayer;

    /** The BiomeCache object for this world. */
    private BiomeCache biomeCache;

    /** A list of biomes that the player can spawn in. */
    private List biomesToSpawnIn;

    private GenLayer temperatureLayer;
    private GenLayer rainfallLayer;
    public BiomeGenBase rendererBiomeGenCache[];
    public float temperatureCache[];
    public double temperature[];
    public double humidity[];
    public double field_4196_c[];
    public OldBiomeGenBase oldField_4195_d[];
    private NoiseGeneratorOctaves2 field_4194_e;
    private NoiseGeneratorOctaves2 field_4193_f;
    private NoiseGeneratorOctaves2 field_4192_g;
    
    protected WorldChunkManager()
    {
        biomeCache = new BiomeCache(this);
        biomesToSpawnIn = new ArrayList();
        biomesToSpawnIn.add(BiomeGenBase.forest);
        biomesToSpawnIn.add(BiomeGenBase.taiga);
        if (mod_noBiomesX.MapFeatures<2){
            biomesToSpawnIn.add(BiomeGenBase.swampland);
        }
        if (mod_noBiomesX.MapFeatures>=2){
            biomesToSpawnIn.add(BiomeGenBase.plains);
            biomesToSpawnIn.add(BiomeGenBase.taigaHills);
            biomesToSpawnIn.add(BiomeGenBase.forestHills);
        }
        if (mod_noBiomesX.MapFeatures==3){
            biomesToSpawnIn.add(BiomeGenBase.field_48416_w);
            biomesToSpawnIn.add(BiomeGenBase.field_48417_x);
        }
    }

    public WorldChunkManager(long par1, WorldType par3WorldType)
    {
        this();
        GenLayer agenlayer[] = GenLayer.func_48425_a(par1, par3WorldType);
        genBiomes = agenlayer[0];
        biomeIndexLayer = agenlayer[1];
        if (mod_noBiomesX.MapFeatures!=3){
            temperatureLayer = agenlayer[2];
            rainfallLayer = agenlayer[3];
        }
    }

    public WorldChunkManager(World par1World)
    {
        this(par1World.getSeed(), par1World.getWorldInfo().getTerrainType());
        field_4194_e = new NoiseGeneratorOctaves2(new Random(par1World.getSeed() * 9871L), 4);
        field_4193_f = new NoiseGeneratorOctaves2(new Random(par1World.getSeed() * 39811L), 4);
        field_4192_g = new NoiseGeneratorOctaves2(new Random(par1World.getSeed() * 0x84a59L), 2);
    }

    /**
     * Gets the list of valid biomes for the player to spawn in.
     */
    public List getBiomesToSpawnIn()
    {
        return biomesToSpawnIn;
    }

    public OldBiomeGenBase oldGetBiomeGenAtChunkCoord(ChunkCoordIntPair chunkcoordintpair)
    {
        return oldGetBiomeGenAt(chunkcoordintpair.chunkXPos << 4, chunkcoordintpair.chunkZPos << 4);
    }

    public OldBiomeGenBase oldGetBiomeGenAt(int i, int j)
    {
        if(mod_noBiomesX.Generator==0)
        {
            return OldBiomeGenBase.notABiome;
        } else
        {
            return oldFunc_4069_a(i, j, 1, 1)[0];
        }
    }

    public OldBiomeGenBase[] oldFunc_4069_a(int i, int j, int k, int l)
    {
        oldField_4195_d = oldLoadBlockGeneratorData(oldField_4195_d, i, j, k, l);
        return oldField_4195_d;
    }

    public OldBiomeGenBase[] oldLoadBlockGeneratorData(OldBiomeGenBase aoldbiomegenbase[], int i, int j, int k, int l)
    {
        if(aoldbiomegenbase == null || aoldbiomegenbase.length < k * l)
        {
            aoldbiomegenbase = new OldBiomeGenBase[k * l];
        }
        temperature = field_4194_e.func_4112_a(temperature, i, j, k, k, 0.02500000037252903D, 0.02500000037252903D, 0.25D);
        humidity = field_4193_f.func_4112_a(humidity, i, j, k, k, 0.05000000074505806D, 0.05000000074505806D, 0.33333333333333331D);
        field_4196_c = field_4192_g.func_4112_a(field_4196_c, i, j, k, k, 0.25D, 0.25D, 0.58823529411764708D);
        int i1 = 0;
        for(int j1 = 0; j1 < k; j1++)
        {
            for(int k1 = 0; k1 < l; k1++)
            {
                double d = field_4196_c[i1] * 1.1000000000000001D + 0.5D;
                double d1 = 0.01D;
                double d2 = 1.0D - d1;
                double d3 = (temperature[i1] * 0.14999999999999999D + 0.69999999999999996D) * d2 + d * d1;
                d1 = 0.002D;
                d2 = 1.0D - d1;
                double d4 = (humidity[i1] * 0.14999999999999999D + 0.5D) * d2 + d * d1;
                d3 = 1.0D - (1.0D - d3) * (1.0D - d3);
                if(d3 < 0.0D)
                {
                    d3 = 0.0D;
                }
                if(d4 < 0.0D)
                {
                    d4 = 0.0D;
                }
                if(d3 > 1.0D)
                {
                    d3 = 1.0D;
                }
                if(d4 > 1.0D)
                {
                    d4 = 1.0D;
                }
                temperature[i1] = d3;
                humidity[i1] = d4;
                aoldbiomegenbase[i1++] = OldBiomeGenBase.getBiomeFromLookup(d3, d4);
            }

        }

        return aoldbiomegenbase;
    }

    public double[] getTemperatures_old(double ad[], int i, int j, int k, int l)
    {
        if(ad == null || ad.length < k * l)
        {
            ad = new double[k * l];
        }
        ad = field_4194_e.func_4112_a(ad, i, j, k, l, 0.02500000037252903D, 0.02500000037252903D, 0.25D);
        field_4196_c = field_4192_g.func_4112_a(field_4196_c, i, j, k, l, 0.25D, 0.25D, 0.58823529411764708D);
        int i1 = 0;
        for(int j1 = 0; j1 < k; j1++)
        {
            for(int k1 = 0; k1 < l; k1++)
            {
                double d = field_4196_c[i1] * 1.1000000000000001D + 0.5D;
                double d1 = 0.01D;
                double d2 = 1.0D - d1;
                double d3 = (ad[i1] * 0.14999999999999999D + 0.69999999999999996D) * d2 + d * d1;
                d3 = 1.0D - (1.0D - d3) * (1.0D - d3);
                if(d3 < 0.0D)
                {
                    d3 = 0.0D;
                }
                if(d3 > 1.0D)
                {
                    d3 = 1.0D;
                }
                ad[i1] = d3;
                i1++;
            }

        }

        return ad;
    }

    public BiomeGenBase getBiomeGenAtChunkCoord(ChunkCoordIntPair chunkcoordintpair)
    {
        return getBiomeGenAt(chunkcoordintpair.chunkXPos << 4, chunkcoordintpair.chunkZPos << 4);
    }

    /**
     * Returns the BiomeGenBase related to the x, z position on the world.
     */
    public BiomeGenBase getBiomeGenAt(int par1, int par2)
    {
        return biomeCache.getBiomeGenAt(par1, par2);
    }

    public float getRainfall(int i, int j)
    {
        return biomeCache.getBiomeCacheBlock(i, j).rainfallValues[i & 0xf | (j & 0xf) << 4];
    }

    /**
     * Returns a list of rainfall values for the specified blocks. Args: listToReuse, x, z, width, length.
     */
    public float[] getRainfall(float par1ArrayOfFloat[], int par2, int par3, int par4, int par5)
    {
        IntCache.resetIntCache();

        if (par1ArrayOfFloat == null || par1ArrayOfFloat.length < par4 * par5)
        {
            par1ArrayOfFloat = new float[par4 * par5];
        }

        int ai[] = biomeIndexLayer.getInts(par2, par3, par4, par5);

        for (int i = 0; i < par4 * par5; i++)
        {
            float f = (float)BiomeGenBase.biomeList[ai[i]].getIntRainfall() / 65536F;

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            par1ArrayOfFloat[i] = f;
        }

        return par1ArrayOfFloat;
    }

    public float getTemperature(int i, int j, int k)
    {
        return getTemperatureAtHeight(biomeCache.getBiomeCacheBlock(i, k).temperatureValues[i & 0xf | (k & 0xf) << 4], j);
    }

    public double getTemperature_old(int i, int j)
    {
        temperature = field_4194_e.func_4112_a(temperature, i, j, 1, 1, 0.02500000037252903D, 0.02500000037252903D, 0.5D);
        return temperature[0];
    }

    public float[] initTemperatureCache(int i, int j, int k, int l)
    {
        temperatureCache = getTemperatures(temperatureCache, i, j, k, l);
        return temperatureCache;
    }

    /**
     * Return an adjusted version of a given temperature based on the y height
     */
    public float getTemperatureAtHeight(float par1, int par2)
    {
        return par1;
    }

    /**
     * Returns a list of temperatures to use for the specified blocks.  Args: listToReuse, x, y, width, length
     */
    public float[] getTemperatures(float par1ArrayOfFloat[], int par2, int par3, int par4, int par5)
    {
        IntCache.resetIntCache();

        if (par1ArrayOfFloat == null || par1ArrayOfFloat.length < par4 * par5)
        {
            par1ArrayOfFloat = new float[par4 * par5];
        }

        int ai[] = biomeIndexLayer.getInts(par2, par3, par4, par5);

        for (int i = 0; i < par4 * par5; i++)
        {
            float f = (float)BiomeGenBase.biomeList[ai[i]].getIntTemperature() / 65536F;

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            par1ArrayOfFloat[i] = f;
        }

        return par1ArrayOfFloat;
    }

    public BiomeGenBase[] func_35557_b(BiomeGenBase abiomegenbase[], int i, int j, int k, int l)
    {
        return getBiomesForGeneration(abiomegenbase, i, j, k, l);
    }

    /**
     * Returns an array of biomes for the location input.
     */
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase par1ArrayOfBiomeGenBase[], int par2, int par3, int par4, int par5)
    {
        IntCache.resetIntCache();

        if (par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5)
        {
            par1ArrayOfBiomeGenBase = new BiomeGenBase[par4 * par5];
        }

        int ai[] = genBiomes.getInts(par2, par3, par4, par5);

        for (int i = 0; i < par4 * par5; i++)
        {
            par1ArrayOfBiomeGenBase[i] = BiomeGenBase.biomeList[ai[i]];
        }

        return par1ArrayOfBiomeGenBase;
    }

    /**
     * Returns biomes to use for the blocks and loads the other data like temperature and humidity onto the
     * WorldChunkManager Args: oldBiomeList, x, z, width, depth
     */
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase par1ArrayOfBiomeGenBase[], int par2, int par3, int par4, int par5)
    {
        return getBiomeGenAt(par1ArrayOfBiomeGenBase, par2, par3, par4, par5, true);
    }

    public BiomeGenBase[] loadRendererData(int i, int j, int k, int l)
    {
        if (k == 16 && l == 16 && (i & 0xf) == 0 && (j & 0xf) == 0)
        {
            return biomeCache.getCachedBiomes(i, j);
        }
        else
        {
            rendererBiomeGenCache = loadBlockGeneratorData(rendererBiomeGenCache, i, j, k, l);
            return rendererBiomeGenCache;
        }
    }

    /**
     * Return a list of biomes for the specified blocks. Args: listToReuse, x, y, width, length, cacheFlag (if false,
     * don't check biomeCache to avoid infinite loop in BiomeCacheBlock)
     */
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase par1ArrayOfBiomeGenBase[], int par2, int par3, int par4, int par5, boolean par6)
    {
        IntCache.resetIntCache();

        if (par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5)
        {
            par1ArrayOfBiomeGenBase = new BiomeGenBase[par4 * par5];
        }

        if (par6 && par4 == 16 && par5 == 16 && (par2 & 0xf) == 0 && (par3 & 0xf) == 0)
        {
            BiomeGenBase abiomegenbase[] = biomeCache.getCachedBiomes(par2, par3);
            System.arraycopy(abiomegenbase, 0, par1ArrayOfBiomeGenBase, 0, par4 * par5);
            return par1ArrayOfBiomeGenBase;
        }

        int ai[] = biomeIndexLayer.getInts(par2, par3, par4, par5);

        for (int i = 0; i < par4 * par5; i++)
        {
            par1ArrayOfBiomeGenBase[i] = BiomeGenBase.biomeList[ai[i]];
        }

        return par1ArrayOfBiomeGenBase;
    }

    /**
     * checks given Chunk's Biomes against List of allowed ones
     */
    public boolean areBiomesViable(int par1, int par2, int par3, List par4List)
    {
        int i = par1 - par3 >> 2;
        int j = par2 - par3 >> 2;
        int k = par1 + par3 >> 2;
        int l = par2 + par3 >> 2;
        int i1 = (k - i) + 1;
        int j1 = (l - j) + 1;
        int ai[] = genBiomes.getInts(i, j, i1, j1);

        for (int k1 = 0; k1 < i1 * j1; k1++)
        {
            BiomeGenBase biomegenbase = BiomeGenBase.biomeList[ai[k1]];

            if (!par4List.contains(biomegenbase))
            {
                return false;
            }
        }

        return true;
    }

    public ChunkPosition func_35556_a(int i, int j, int k, List list, Random random)
    {
        int l = i - k >> 2;
        int i1 = j - k >> 2;
        int j1 = i + k >> 2;
        int k1 = j + k >> 2;
        int l1 = (j1 - l) + 1;
        int i2 = (k1 - i1) + 1;
        int ai[] = genBiomes.getInts(l, i1, l1, i2);
        ChunkPosition chunkposition = null;
        int j2 = 0;
        for (int k2 = 0; k2 < ai.length; k2++)
        {
            int l2 = l + k2 % l1 << 2;
            int i3 = i1 + k2 / l1 << 2;
            BiomeGenBase biomegenbase = BiomeGenBase.biomeList[ai[k2]];
            if (list.contains(biomegenbase) && (chunkposition == null || random.nextInt(j2 + 1) == 0))
            {
                chunkposition = new ChunkPosition(l2, 0, i3);
                j2++;
            }
        }

        return chunkposition;
    }

    /**
     * Finds a valid position within a range, that is once of the listed biomes.
     */
    public ChunkPosition findBiomePosition(int par1, int par2, int par3, List par4List, Random par5Random)
    {
        int i = par1 - par3 >> 2;
        int j = par2 - par3 >> 2;
        int k = par1 + par3 >> 2;
        int l = par2 + par3 >> 2;
        int i1 = (k - i) + 1;
        int j1 = (l - j) + 1;
        int ai[] = genBiomes.getInts(i, j, i1, j1);
        ChunkPosition chunkposition = null;
        int k1 = 0;

        for (int l1 = 0; l1 < ai.length; l1++)
        {
            int i2 = i + l1 % i1 << 2;
            int j2 = j + l1 / i1 << 2;
            BiomeGenBase biomegenbase = BiomeGenBase.biomeList[ai[l1]];

            if (par4List.contains(biomegenbase) && (chunkposition == null || par5Random.nextInt(k1 + 1) == 0))
            {
                chunkposition = new ChunkPosition(i2, 0, j2);
                k1++;
            }
        }

        return chunkposition;
    }

    /**
     * Calls the WorldChunkManager's biomeCache.cleanupCache()
     */
    public void cleanupCache()
    {
        biomeCache.cleanupCache();
    }

    public ChunkPosition func_35556_a_ignoreBiome(int i, int j, int k, List list, Random random)
    {
        int l = i - k >> 2;
        int i1 = j - k >> 2;
        int j1 = i + k >> 2;
        int k1 = j + k >> 2;
        int l1 = (j1 - l) + 1;
        int i2 = (k1 - i1) + 1;
        int ai[] = genBiomes.getInts(l, i1, l1, i2);
        ChunkPosition chunkposition = null;
        int j2 = 0;
        for(int k2 = 0; k2 < ai.length; k2++)
        {
            int l2 = l + k2 % l1 << 2;
            int i3 = i1 + k2 / l1 << 2;
            BiomeGenBase biomegenbase = BiomeGenBase.biomeList[ai[k2]];
            if(chunkposition == null || random.nextInt(j2 + 1) == 0)
            {
                chunkposition = new ChunkPosition(l2, 0, i3);
                j2++;
            }
        }

        return chunkposition;
    }
}