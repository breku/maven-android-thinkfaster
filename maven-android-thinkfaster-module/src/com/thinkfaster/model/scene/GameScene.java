package com.thinkfaster.model.scene;

import com.thinkfaster.manager.ResourcesManager;
import com.thinkfaster.manager.SceneManager;
import com.thinkfaster.matcher.ClassTouchAreaMacher;
import com.thinkfaster.model.shape.GameButton;
import com.thinkfaster.model.shape.LifeBar;
import com.thinkfaster.model.shape.MathEquation;
import com.thinkfaster.model.shape.MathEquationText;
import com.thinkfaster.pool.MathEquationPool;
import com.thinkfaster.service.HighScoreService;
import com.thinkfaster.util.ConstantsUtil;
import com.thinkfaster.util.LevelDifficulty;
import com.thinkfaster.util.MathParameter;
import com.thinkfaster.util.SceneType;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.MoveByModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;

import java.util.ArrayDeque;

/**
 * User: Breku
 * Date: 21.09.13
 */
public class GameScene extends BaseScene {

    private HUD gameHUD;

    private LevelDifficulty levelDifficulty;
    private MathParameter mathParameter;
    private MathEquationPool pool;

    private GameButton redButton;
    private GameButton greenButton;
    private LifeBar lifeBar;

    private Text textNumberOfGoodClicks;
    private Text textNumberOfWrongClicks;

    // Head of the queue is on the bottom
    private ArrayDeque<MathEquationText> mathEquationTextQueue;

    private Integer numberOfGoodClicks;
    private Integer numberOfWrongClicks;

    private HighScoreService highScoreService;

    private Integer firstTimeCounter;

    private Boolean multiplayer;

    /**
     * @param objects objects[0] - levelDifficulty
     *                objects[1] - mathParameter
     *                objects[2] - multiplayer
     */
    public GameScene(Object... objects) {
        super(objects);
    }


    @Override
    public void createScene(Object... objects) {
        init(objects);
        createBackground();
        initAfterBackgroundCreation();
        createHUD();
    }


    @Override
    public void onBackKeyPressed() {
        SceneManager.getInstance().loadMenuSceneFrom(SceneType.GAME);
    }

    private void init(Object... objects) {
        clearUpdateHandlers();
        clearTouchAreas();

        numberOfGoodClicks = 0;
        numberOfWrongClicks = 0;

        firstTimeCounter = 0;

        levelDifficulty = (LevelDifficulty) objects[0];
        mathParameter = (MathParameter) objects[1];
        multiplayer = (Boolean) objects[2];

        pool = new MathEquationPool(levelDifficulty, mathParameter);
        pool.batchAllocatePoolItems(ConstantsUtil.INITIAL_POOL_SIZE);

        highScoreService = new HighScoreService();

    }

    private void initAfterBackgroundCreation() {
        mathEquationTextQueue = getEquationsTexts();
    }


    private void createHUD() {
        gameHUD = new HUD();
        camera.setHUD(gameHUD);
    }

    private void createBackground() {
        unregisterTouchAreas(new ClassTouchAreaMacher(Sprite.class));
        clearChildScene();
        attachChild(new Sprite(ConstantsUtil.SCREEN_WIDTH / 2, ConstantsUtil.SCREEN_HEIGHT / 2,
                ResourcesManager.getInstance().getBackgroundGameTextureRegion(), vertexBufferObjectManager));
        createInitialEquations();
        createGreenButton();
        createRedButton();
        createLifeBar();
        createScoreTexts();
    }

    private void createScoreTexts() {
        Text textGoodClicks = new Text(700, 380, ResourcesManager.getInstance().getWhiteFont(), "Good clicks:", vertexBufferObjectManager);
        Text textWrongClicks = new Text(100, 380, ResourcesManager.getInstance().getWhiteFont(), "Wrong clicks:", vertexBufferObjectManager);

        textGoodClicks.setScale(0.7f);
        textWrongClicks.setScale(0.7f);

        textNumberOfGoodClicks = new Text(700, 320, ResourcesManager.getInstance().getWhiteFont(), "0000", vertexBufferObjectManager);
        textNumberOfWrongClicks = new Text(100, 320, ResourcesManager.getInstance().getWhiteFont(), "0000", vertexBufferObjectManager);
        textNumberOfGoodClicks.setText("0");
        textNumberOfWrongClicks.setText("0");

        attachChild(textGoodClicks);
        attachChild(textWrongClicks);
        attachChild(textNumberOfGoodClicks);
        attachChild(textNumberOfWrongClicks);
    }

    private void createLifeBar() {
        lifeBar = new LifeBar();
        lifeBar.registerEntityModifier(new LoopEntityModifier(
                new MoveByModifier(1.0f, levelDifficulty.getLifeBarSpeed(), 0)
        ));

        attachChild(lifeBar);
        Sprite lifeBarBorder = new Sprite(ConstantsUtil.SCREEN_WIDTH / 2, 460, ResourcesManager.getInstance().getLifeBarBorderTextureRegion(), vertexBufferObjectManager);
        lifeBarBorder.setZIndex(10);
        attachChild(lifeBarBorder);

    }

    private void createInitialEquations() {
        for (int i = 0; i < 4; i++) {
            attachChild(new MathEquationText(400, i * 80 + 140, pool.obtainPoolItem()));
        }
    }

    private void createRedButton() {
        redButton = new GameButton(100, 100, ResourcesManager.getInstance().getButtonNoTextureRegion());
        registerTouchArea(redButton);
        attachChild(redButton);
    }

    private void createGreenButton() {
        greenButton = new GameButton(700, 100, ResourcesManager.getInstance().getButtonOkTextureRegion());
        registerTouchArea(greenButton);
        attachChild(greenButton);
    }

    private ArrayDeque<MathEquationText> getEquationsTexts() {
        ArrayDeque<MathEquationText> result = new ArrayDeque<MathEquationText>();
        for (IEntity entity : mChildren) {
            if (entity instanceof MathEquationText) {
                result.add((MathEquationText) entity);
            }
        }
        return result;
    }


    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (greenButton.isClicked()) {
            greenButton.clearState();
            MathEquation mathEquation = mathEquationTextQueue.peek().getMathEquation();
            if (mathEquation.isCorrect()) {
                lifeBar.goodClick();
                addToGoodClicks();
                resourcesManager.getGoodClickSound().play();
            } else {
                lifeBar.wrongClick();
                addToWrongClicks();
                resourcesManager.getWrongClickSound().play();
            }
            manageElementsAfterClick();
        }
        if (redButton.isClicked()) {
            redButton.clearState();
            MathEquation mathEquation = mathEquationTextQueue.peek().getMathEquation();
            if (mathEquation.isCorrect()) {
                lifeBar.wrongClick();
                addToWrongClicks();
                resourcesManager.getWrongClickSound().play();
            } else {
                lifeBar.goodClick();
                addToGoodClicks();
                resourcesManager.getGoodClickSound().play();
            }
            manageElementsAfterClick();
        }
        updateLifeBar();

        if (firstTimeCounter++ == 1) {
            resourcesManager.getStartGameSound().play();
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    private void updateLifeBar() {
        if (lifeBar.isEnd()) {
            Integer score = numberOfGoodClicks - numberOfWrongClicks;
            if (highScoreService.isHighScore(levelDifficulty, mathParameter, score)) {
                endGameWin();
            } else if (score >= ConstantsUtil.MINIMUM_SCORE_TO_UNLOCK_LEVEL) {
                endGameHalfWin();
            } else {
                endGameLose();
            }
        }
    }

    private void endGameHalfWin() {
        Integer score = numberOfGoodClicks - numberOfWrongClicks;
        resourcesManager.getHalfWinSound().play();
        SceneManager.getInstance().loadEndGameScene(score);
    }

    private void endGameWin() {
        Integer score = numberOfGoodClicks - numberOfWrongClicks;
        highScoreService.updateRecordFor(levelDifficulty, mathParameter, score);
        if (score > ConstantsUtil.MINIMUM_SCORE_TO_UNLOCK_LEVEL) {
            highScoreService.unlockLevelUpFor(levelDifficulty, mathParameter);
        }
        resourcesManager.getWinSound().play();
        SceneManager.getInstance().loadHighScoreSceneFrom(SceneType.GAME, score, levelDifficulty, mathParameter);
    }

    private void endGameLose() {
        Integer score = numberOfGoodClicks - numberOfWrongClicks;
        resourcesManager.getLoseSound().play();
        SceneManager.getInstance().loadEndGameScene(score);
    }

    private void manageElementsAfterClick() {
        moveAllElements();
        removeBottomElement();
        addNewTopElement();
        sortChildren();
    }

    private void moveAllElements() {
        // Starts from head - bottom
        for (MathEquationText text : mathEquationTextQueue) {
            text.registerEntityModifier(new MoveYModifier(ConstantsUtil.TEXT_MOVE_TIME, text.getY(), text.getY() - 80));
        }
    }

    private void removeBottomElement() {
        MathEquationText bottomElement = mathEquationTextQueue.poll();
        detachChild(bottomElement);
    }

    private void addNewTopElement() {
        MathEquationText text = new MathEquationText(400, 460, pool.obtainPoolItem());
        text.registerEntityModifier(new MoveYModifier(ConstantsUtil.TEXT_MOVE_TIME, text.getY(), text.getY() - 80));
        mathEquationTextQueue.add(text);
        attachChild(text);
    }

    @Override
    public SceneType getSceneType() {
        return SceneType.GAME;
    }

    @Override
    public void disposeScene() {
        gameHUD.clearChildScene();
        camera.setHUD(null);
        camera.setCenter(ConstantsUtil.SCREEN_WIDTH / 2, ConstantsUtil.SCREEN_HEIGHT / 2);
        camera.setChaseEntity(null);
    }

    private void addToGoodClicks() {
        numberOfGoodClicks++;
        textNumberOfGoodClicks.setText(numberOfGoodClicks.toString());
    }

    private void addToWrongClicks() {
        numberOfWrongClicks++;
        textNumberOfWrongClicks.setText(numberOfWrongClicks.toString());
    }
}
