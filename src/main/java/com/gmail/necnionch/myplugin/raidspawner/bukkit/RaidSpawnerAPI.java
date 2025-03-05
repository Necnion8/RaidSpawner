package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.Action;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.ActionProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.LandAction;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.Condition;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerConfig;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.RaidSpawnerLang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.map.ChunkViewRenderer;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.LandChunkFindResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndReason;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RaidSpawnerAPI {

    /**
     * プラグインの設定を返します
     */
    RaidSpawnerConfig getPluginConfig();

    /**
     * プラグインの言語を返します
     */
    RaidSpawnerLang getPluginLang();

    /**
     * 現在の襲撃中および敗北した {@link RaidSpawner} を返します<br>
     * このリストはすべての襲撃イベントが終了するまでリセットされません。
     */
    Map<Land, RaidSpawner> getCurrentRaids();

    /**
     * いずれかの襲撃イベントを実行している場合は true を返す
     */
    boolean isRunningRaid();

    /**
     * 指定されたLandで襲撃イベントを実行している場合は true を返す
     */
    boolean isRunningRaid(Land land);

    /**
     * 全てのLandで襲撃イベントを開始します
     * @return いずれかのLandでイベントが開始できたら true
     * @throws IllegalStateException すでにいずれか襲撃イベントが開始している
     * @throws IllegalArgumentException Landスポーン地点が設定されていない
     */
    boolean startRaidAll(@Nullable Condition reason) throws IllegalArgumentException ;

    /**
     * 実行中の襲撃イベントをすべて中止します
     */
    void clearRaidAll(@Nullable RaidEndResult result, @Nullable RaidEndReason reason);

    /**
     * 指定されたLandで襲撃イベントを開始します
     * @return 開始できたら true
     * @throws IllegalStateException すでに襲撃イベントが開始しているか、または不明な {@link Land} であるとき
     * @throws IllegalArgumentException Landスポーン地点が設定されていない
     */
    boolean startRaid(Land land);

    /**
     * イベントの自動開始タイマーが有効なら true を返します
     */
    boolean isStandbyAutoStart();

    /**
     * 自動開始条件のリストを返します
     */
    List<ConditionWrapper> getAutoStartConditions();

    /**
     * タイプID と {@link Condition} を作成するプロバイダのマップ
     */
    Map<String, ConditionProvider<?>> conditionProviders();

    /**
     * タイプID と {@link LandAction} を作成するプロバイダのマップ
     */
    Map<String, ActionProvider<?>> landActionProviders();

    /**
     * タイプID と {@link PlayerAction} を作成するプロバイダのマップ
     */
    Map<String, ActionProvider<?>> playerActionProviders();

    /**
     * タイプID と {@link Enemy} を作成するプロバイダのマップ
     */
    Map<String, EnemyProvider<?>> enemyProviders();

    /**
     * プラグインがチャンクの読み込みを強制しているチャンクのリストを返す
     */
    Multimap<World, Chunk> getChunkTickets();

    /**
     * Landチャンクに基づいて襲撃に必要なチャンクを収集します
     * @param lands 襲撃を実行するLand
     * @throws IllegalArgumentException 襲撃に設定されているワールドが存在しない
     */
    List<LandChunkFindResult> findLandChunk(@Nullable Collection<Land> lands) throws IllegalArgumentException;

    /**
     * 前回の findLandChunk の結果を返します
     */
    @Nullable List<LandChunkFindResult> getLastLandChunkFindResult();

    /**
     * {@link ChunkViewRenderer} を取得します
     */
    ChunkViewRenderer getChunkViewRenderer();

    /**
     * 有効な {@link ChunkViewRenderer} のチャンク情報を再読み込みします
     */
    void updateChunkViewRendererChunks();

    /**
     * 終了結果に基づいて実行アクションを実行します
     * @return 実行されたアクションタイプと結果のマップ
     */
    @Nullable Map<Class<Action>, Boolean> executeActions(RaidSpawner spawner, RaidEndResult result);

    /**
     * 指定された実行アクションを実行します
     * @return 実行されたアクションタイプと結果のマップ
     */
    Map<Class<Action>, Boolean> executeActions(RaidSpawner spawner, List<Action> actions);

    Collection<Land> getLands();

    LandsIntegration getLandAPI();

}
