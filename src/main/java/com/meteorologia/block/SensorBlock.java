package com.meteorologia.block;

import com.meteorologia.weather.StormManager;
import com.meteorologia.weather.StormWorld;
import com.meteorologia.weather.WindModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sensores meteorologicos. Clique direito mostra a leitura; emitem sinal de
 * redstone proporcional (anemometro = vento, barometro = queda de pressao,
 * termometro = temperatura, estacao = nivel de perigo).
 */
public class SensorBlock extends Block {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public enum Kind {
        ANEMOMETER(Block.box(0, 0, 0, 16, 12, 16)),
        BAROMETER(Block.box(2, 0, 2, 14, 12, 14)),
        THERMOMETER(Block.box(1, 0, 1, 15, 14, 15)),
        STATION(Block.box(1, 0, 1, 15, 16, 15));

        final VoxelShape shape;

        Kind(VoxelShape shape) {
            this.shape = shape;
        }
    }

    private final Kind kind;

    public SensorBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind.shape;
    }

    // ------------------------------------------------------------ redstone --
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(LEVEL);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int signal = signal(level, pos);
        if (signal != state.getValue(LEVEL)) {
            level.setBlock(pos, state.setValue(LEVEL, signal), 3);
        }
        level.scheduleTick(pos, this, 20);
    }

    // ------------------------------------------------------------- leituras --
    private int signal(ServerLevel level, BlockPos pos) {
        StormWorld w = StormManager.get(level);
        switch (kind) {
            case ANEMOMETER: {
                double[] wind = WindModel.wind(w, level.getGameTime(), pos.getX(), pos.getY(), pos.getZ());
                return clamp15(WindModel.speed(wind) / 10.0);
            }
            case BAROMETER: {
                double p = WindModel.pressureHpa(level, w, pos);
                return clamp15((1020.0 - p) / 60.0 * 15.0);
            }
            case THERMOMETER: {
                double t = WindModel.temperatureC(level, pos);
                return clamp15((t + 10.0) / 50.0 * 15.0);
            }
            default: {
                int warn = StormManager.warningLevel(level, pos.getX(), pos.getZ());
                return warn >= 2 ? 15 : warn == 1 ? 8 : (level.isRaining() ? 3 : 0);
            }
        }
    }

    private static int clamp15(double v) {
        return (int) Math.max(0, Math.min(15, Math.round(v)));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.sidedSuccess(true);
        }
        player.displayClientMessage(Component.literal(readout(sl, pos)), false);
        if (kind == Kind.STATION) {
            for (String line : StormManager.report(sl, pos.getX(), pos.getZ())) {
                player.displayClientMessage(Component.literal(line), false);
            }
        }
        return InteractionResult.sidedSuccess(false);
    }

    /** Texto de leitura (tambem usado pelo Medidor Portatil). */
    public static String fullReading(ServerLevel level, BlockPos pos) {
        StormWorld w = StormManager.get(level);
        double[] wind = WindModel.wind(w, level.getGameTime(), pos.getX(), pos.getY(), pos.getZ());
        return String.format("§bVento: §f%s §7| §bPressao: §f%d hPa §7| §bTemp: §f%.1f C §7| §bUmidade: §f%d%%",
                WindModel.describeWind(wind),
                (int) Math.round(WindModel.pressureHpa(level, w, pos)),
                WindModel.temperatureC(level, pos),
                (int) Math.round(WindModel.humidityPct(level, pos)));
    }

    private String readout(ServerLevel level, BlockPos pos) {
        StormWorld w = StormManager.get(level);
        switch (kind) {
            case ANEMOMETER: {
                double[] wind = WindModel.wind(w, level.getGameTime(), pos.getX(), pos.getY(), pos.getZ());
                return "§bVento: §f" + WindModel.describeWind(wind);
            }
            case BAROMETER:
                return String.format("§bPressao: §f%d hPa", (int) Math.round(WindModel.pressureHpa(level, w, pos)));
            case THERMOMETER:
                return String.format("§bTemperatura: §f%.1f C §7| §bUmidade: §f%d%%",
                        WindModel.temperatureC(level, pos), (int) Math.round(WindModel.humidityPct(level, pos)));
            default:
                return fullReading(level, pos);
        }
    }
}
