/*
 * Copyright (c) 2023 by The VASSAL Development team, Brian Reynolds
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.counters;

import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.build.module.map.boardPicker.board.MapGrid;
import VASSAL.build.module.map.boardPicker.board.RegionGrid;
import VASSAL.build.module.map.boardPicker.board.ZonedGrid;
import VASSAL.command.Command;
import VASSAL.configure.BooleanConfigurer;
import VASSAL.configure.IntConfigurer;
import VASSAL.configure.NamedHotKeyConfigurer;
import VASSAL.configure.PropertyExpression;
import VASSAL.configure.StringConfigurer;
import VASSAL.i18n.Resources;
import VASSAL.i18n.TranslatablePiece;
import VASSAL.search.SearchTarget;
import VASSAL.tools.FormattedString;
import VASSAL.tools.NamedKeyStroke;
import VASSAL.tools.SequenceEncoder;
import VASSAL.tools.imageop.GamePieceOp;
import VASSAL.tools.imageop.Op;
import VASSAL.tools.imageop.RotateScaleOp;

import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static VASSAL.counters.BasicPiece.BASIC_NAME;
import static VASSAL.counters.BasicPiece.CURRENT_BOARD;
import static VASSAL.counters.BasicPiece.CURRENT_MAP;
import static VASSAL.counters.BasicPiece.CURRENT_ZONE;
import static VASSAL.counters.BasicPiece.LOCATION_NAME;
import static VASSAL.counters.BasicPiece.PIECE_NAME;

/**
 * Designates the piece as "Cargo", which can be placed on a "Mat" to move along with it
 */
public class LocationCommand extends Decorator implements TranslatablePiece {
  public static final String ID = "locCommand;"; // NON-NLS

  public static final String LOC_NAME = "Location";

  public static final String LOC_REGIONS = "locRegions"; //NON-NLS
  public static final String LOC_ZONES   = "locZones"; //NON-NLS
  public static final String[] LOC_OPTIONS = { LOC_REGIONS, LOC_ZONES };
  public static final String[] LOC_KEYS    = { "Editor.LocationCommand.regions", "Editor.LocationCommand.zones"};


  // Type variables (configured in Ed)
  protected String desc;
  protected String locType;
  protected PropertyExpression propertiesFilter = new PropertyExpression();
  protected FormattedString menuText;
  protected NamedKeyStroke key;

  // Private stuff
  private List<LocationKeyCommand> keyCommands = new ArrayList<>();


  private class LocationKeyCommand extends KeyCommand {
    private String locationName;

    public LocationKeyCommand(String name, NamedKeyStroke key, GamePiece target, TranslatablePiece i18nPiece, String locationName) {
      super(name, key, target, i18nPiece);
      this.locationName = locationName;
    }

    public String getLocationName() {
      return locationName;
    }
  }



  public LocationCommand() {
    this(ID + ";", null); //NON-NLS
  }

  public LocationCommand(String type, GamePiece inner) {
    mySetType(type);
    setInner(inner);
  }


  @Override
  public void mySetType(String type) {
    type = type.substring(ID.length());
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(type, ';');
    desc = st.nextToken("");
    locType = st.nextToken(LOC_REGIONS);
    propertiesFilter.setExpression(st.nextToken(""));
    menuText.setFormat(st.nextToken(Resources.getString("Editor.LocationCommand.loc_default_command")));
    key = st.nextNamedKeyStroke();
  }

  @Override
  public String myGetType() {
    final SequenceEncoder se = new SequenceEncoder(';');
    se.append(desc)
      .append(locType)
      .append(propertiesFilter.getExpression())
      .append(menuText.getFormat())
      .append(key);
    return ID + se.getValue();
  }

  /**
   * @return a list of any Named KeyStrokes referenced in the Decorator, if any (for search)
   */
  @Override
  public List<NamedKeyStroke> getNamedKeyStrokeList() {
    return Arrays.asList(key);
  }

  /**
   * {@link SearchTarget}
   * @return a list of any Menu/Button/Tooltip Text strings referenced in the Decorator, if any (for search)
   */
  @Override
  public List<String> getMenuTextList() {
    return Arrays.asList(menuText.getFormat());
  }

  /**
   * {@link SearchTarget}
   * @return a list of the Decorator's string/expression fields if any (for search)
   */
  @Override
  public List<String> getExpressionList() {
    return Arrays.asList(propertiesFilter.getExpression());
  }


  @Override
  public KeyCommand[] myGetKeyCommands() {
    if ((key == null) || key.isNull()) {
      return KeyCommand.NONE;
    }

    keyCommands.clear();

    final Map map = getMap();
    if (map == null) return KeyCommand.NONE;
    for (final Board board : map.getBoardPicker().getSelectedBoards()) {
      final MapGrid grid = board.getGrid();
      if (!(grid instanceof ZonedGrid) && (!(grid instanceof RegionGrid) || )) continue;

    }

    return (KeyCommand[]) keyCommands.toArray();
  }


  @Override
  public String myGetState() {
    final SequenceEncoder se = new SequenceEncoder(';');
    //se.append(mat == null ? NO_MAT : mat.getId());
    return se.getValue();
  }

  @Override
  public Command myKeyEvent(KeyStroke stroke) {
    if (!keyCommandsSet) {
      if (matFindKey != null && !matFindKey.isNull()) {
        matFindKeyCommand = new KeyCommand("", matFindKey, Decorator.getOutermost(this), this);
      }
      if (matDetachKey != null && !matDetachKey.isNull()) {
        matDetachKeyCommand = new KeyCommand("", matDetachKey, Decorator.getOutermost(this), this);
      }
      keyCommandsSet = true;
    }

    // Our Find command
    if ((matFindKeyCommand != null) && matFindKeyCommand.matches(stroke)) {
      return findNewMat();
    }

    // Our Detach command detaches us from our mat.
    if ((matDetachKeyCommand != null) && matDetachKeyCommand.matches(stroke)) {
      return makeClearMatCommand();
    }

    return null;
  }

  @Override
  public void mySetState(String newState) {
    final GameModule gm = GameModule.getGameModule();

    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(newState, ';');
    final String token = st.nextToken();

    mat = NO_MAT.equals(token) ? null : gm.getGameState().getPieceForId(token);
    setMat(mat); //BR// This makes sure the Mat also knows about us. (if it loaded from saved game before us and couldn't find us yet)

    gm.setMatSupport(true);
  }

  @Override
  public Rectangle boundingBox() {
    final Rectangle b = piece.boundingBox();
    final double angle = getMatAngle();

    if (angle == 0.0) {
      return b;
    }

    Rectangle r;
    if ((getGpOp() != null && getGpOp().isChanged()) ||
        (r = bounds.get(angle)) == null) {
      r = AffineTransform.getRotateInstance(-PI_180 * angle,
                                            centerX(),
                                            centerY())
                         .createTransformedShape(b).getBounds();
      bounds.put(angle, r);
    }

    return new Rectangle(r);
  }

  protected GamePieceOp getGpOp() {
    if (gpOp == null) {
      if (getInner() != null) {
        gpOp = Op.piece(getInner());
      }
    }
    return gpOp;
  }

  protected double getMatAngle() {
    if (mat == null || !maintainRelativeFacing) {
      return 0.0;
    }

    final FreeRotator mrot = (FreeRotator) Decorator.getDecorator(getOutermost(mat), FreeRotator.class);
    return mrot == null ? 0.0 : mrot.getAngle();
  }

  public double getMatAngleInRadians() {
    return -PI_180 * getMatAngle();
  }

  /**
   * If we're maintaining facing to our Mat, rotate our graphics as appropriate to account for that when drawing
   */
  @Override
  public void draw(Graphics g, int x, int y, Component obs, double zoom) {
    final double angle = getMatAngle();
    if (angle == 0.0) {
      piece.draw(g, x, y, obs, zoom);
      return;
    }

    RotateScaleOp op;
    if (getGpOp() != null && getGpOp().isChanged()) {
      gpOp = Op.piece(piece);
      bounds.clear();
      rotOp.clear();
      op = Op.rotateScale(gpOp, angle, zoom);
      rotOp.put(angle, op);
    }
    else {
      op = rotOp.get(angle);
      if (op == null || op.getScale() != zoom) {
        op = Op.rotateScale(gpOp, angle, zoom);
        rotOp.put(angle, op);
      }
    }

    final Rectangle r = boundingBox();

    final Image img = op.getImage();
    if (img != null) {
      g.drawImage(img, x + (int) (zoom * r.x), y + (int) (zoom * r.y), obs);
    }
  }

  @Override
  public String getName() {
    return piece.getName();
  }

  private double centerX() {
    // The center is not on a vertex for pieces with odd widths.
    return (piece.boundingBox().width % 2) / 2.0;
  }

  private double centerY() {
    // The center is not on a vertex for pieces with odd heights.
    return (piece.boundingBox().height % 2) / 2.0;
  }

  /**
   * If we're maintaining facing to our Mat, rotate our piece's shape to account for that.
   * @return Properly rotated shape
   */
  @Override
  public Shape getShape() {
    final double angle = getMatAngle();
    final Shape s = piece.getShape();

    if (angle == 0.0) {
      return s;
    }

    return AffineTransform.getRotateInstance(
      -PI_180 * angle, centerX(), centerY()
    ).createTransformedShape(s);
  }

  @Override
  public PieceEditor getEditor() {
    return new Ed(this);
  }

  @Override
  public Object getProperty(Object key) {
    if (mat != null) {
      if (CURRENT_MAT.equals(key)) {
        return mat.getProperty(Mat.MAT_NAME);
      }
      else if (Properties.IGNORE_GRID.equals(key) && mat != null) {
        return true;
      }
      else if (CURRENT_MAT_ID.equals(key)) {
        return mat.getProperty(Mat.MAT_ID);
      }
      else if (CURRENT_MAT_X.equals(key)) {
        return Decorator.getOutermost(mat).getPosition().x;
      }
      else if (CURRENT_MAT_Y.equals(key)) {
        return Decorator.getOutermost(mat).getPosition().y;
      }
      else if (CURRENT_MAT_OFFSET_X.equals(key)) {
        return Decorator.getOutermost(mat).getPosition().x - Decorator.getOutermost(this).getPosition().x;
      }
      else if (CURRENT_MAT_OFFSET_Y.equals(key)) {
        return Decorator.getOutermost(mat).getPosition().y - Decorator.getOutermost(this).getPosition().y;
      }
      else if (CURRENT_MAT_BASIC_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(BASIC_NAME);
      }
      else if (CURRENT_MAT_PIECE_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(PIECE_NAME);
      }
      else if (CURRENT_MAT_LOCATION_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(LOCATION_NAME);
      }
      else if (CURRENT_MAT_ZONE.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(CURRENT_ZONE);
      }
      else if (CURRENT_MAT_BOARD.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(CURRENT_BOARD);
      }
      else if (CURRENT_MAT_MAP.equals(key)) {
        return Decorator.getOutermost(mat).getProperty(CURRENT_MAP);
      }
      else if (List.of(CURRENT_MAT_PROP0, CURRENT_MAT_PROP1, CURRENT_MAT_PROP2, CURRENT_MAT_PROP3, CURRENT_MAT_PROP4, CURRENT_MAT_PROP5, CURRENT_MAT_PROP6, CURRENT_MAT_PROP7, CURRENT_MAT_PROP8, CURRENT_MAT_PROP9
      ).contains(key)) {
        return Decorator.getOutermost(mat).getProperty(key);
      }
    }

    if (IS_CARGO.equals(key)) {
      return Boolean.TRUE;
    }
    return super.getProperty(key);
  }

  @Override
  public Object getLocalizedProperty(Object key) {
    if (mat != null) {
      if (CURRENT_MAT.equals(key)) {
        return mat.getLocalizedProperty(Mat.MAT_NAME);
      }
      else if (Properties.IGNORE_GRID.equals(key) && mat != null) {
        return true;
      }
      else if (CURRENT_MAT_BASIC_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(BASIC_NAME);
      }
      else if (CURRENT_MAT_PIECE_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(PIECE_NAME);
      }
      else if (CURRENT_MAT_LOCATION_NAME.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(LOCATION_NAME);
      }
      else if (CURRENT_MAT_ZONE.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(CURRENT_ZONE);
      }
      else if (CURRENT_MAT_BOARD.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(CURRENT_BOARD);
      }
      else if (CURRENT_MAT_MAP.equals(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(CURRENT_MAP);
      }
      else if (List.of(CURRENT_MAT_PROP0, CURRENT_MAT_PROP1, CURRENT_MAT_PROP2, CURRENT_MAT_PROP3, CURRENT_MAT_PROP4, CURRENT_MAT_PROP5, CURRENT_MAT_PROP6, CURRENT_MAT_PROP7, CURRENT_MAT_PROP8, CURRENT_MAT_PROP9
      ).contains(key)) {
        return Decorator.getOutermost(mat).getLocalizedProperty(key);
      }
    }

    if (List.of(
      CURRENT_MAT_ID,
      CURRENT_MAT_X,
      CURRENT_MAT_Y,
      CURRENT_MAT_OFFSET_X,
      CURRENT_MAT_OFFSET_Y,
      IS_CARGO
    ).contains(key)) {
      return getProperty(key);
    }
    return super.getLocalizedProperty(key);
  }

  @Override
  public String getDescription() {
    return buildDescription("Editor.MatCargo.trait_description", desc);
  }

  @Override
  public String getBaseDescription() {
    return Resources.getString("Editor.MatCargo.trait_description");
  }

  @Override
  public String getDescriptionField() {
    return desc;
  }

  @Override
  public boolean testEquals(Object o) {
    if (! (o instanceof LocationCommand)) return false;
    final LocationCommand c = (LocationCommand) o;
    if (detectionDistanceX != c.detectionDistanceX) return false;
    if (detectionDistanceY != c.detectionDistanceY) return false;
    if (!Objects.equals(matFindKey, c.matFindKey)) return false;
    if (!Objects.equals(matDetachKey, c.matDetachKey)) return false;
    return Objects.equals(desc, c.desc) &&
           maintainRelativeFacing == c.maintainRelativeFacing;
  }

  @Override
  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("MatCargo.html"); // NON-NLS
  }

  /**
   * Return Property names exposed by this trait
   */
  @Override
  public List<String> getPropertyNames() {
    return Arrays.asList(CURRENT_MAT, CURRENT_MAT_ID, IS_CARGO, CURRENT_MAT_X, CURRENT_MAT_Y, CURRENT_MAT_OFFSET_X, CURRENT_MAT_OFFSET_Y, CURRENT_MAT_BASIC_NAME, CURRENT_MAT_PIECE_NAME, CURRENT_MAT_LOCATION_NAME, CURRENT_MAT_ZONE, CURRENT_MAT_BOARD, CURRENT_MAT_MAP, CURRENT_MAT_PROP0, CURRENT_MAT_PROP1, CURRENT_MAT_PROP2, CURRENT_MAT_PROP3, CURRENT_MAT_PROP4, CURRENT_MAT_PROP5, CURRENT_MAT_PROP6, CURRENT_MAT_PROP7, CURRENT_MAT_PROP8, CURRENT_MAT_PROP9);
  }

  public static class Ed implements PieceEditor {
    private final StringConfigurer descInput;
    private final BooleanConfigurer rotInput;
    private final TraitConfigPanel controls;
    private final IntConfigurer xInput;
    private final IntConfigurer yInput;
    private final NamedHotKeyConfigurer findInput;
    private final NamedHotKeyConfigurer detachInput;

    public Ed(LocationCommand p) {
      controls = new TraitConfigPanel();

      descInput = new StringConfigurer(p.desc);
      descInput.setHintKey("Editor.description_hint");
      controls.add("Editor.description_label", descInput);

      rotInput = new BooleanConfigurer(p.maintainRelativeFacing);
      controls.add("Editor.MatCargo.maintain_relative_facing", rotInput);

      xInput = new IntConfigurer(p.detectionDistanceX);
      controls.add("Editor.MatCargo.detection_distance_x", xInput);

      yInput = new IntConfigurer(p.detectionDistanceY);
      controls.add("Editor.MatCargo.detection_distance_y", yInput);

      findInput = new NamedHotKeyConfigurer(p.matFindKey);
      controls.add("Editor.MatCargo.mat_find_key", findInput);

      detachInput = new NamedHotKeyConfigurer(p.matDetachKey);
      controls.add("Editor.MatCargo.mat_detach_key", detachInput);
    }

    @Override
    public Component getControls() {
      return controls;
    }

    @Override
    public String getType() {
      final SequenceEncoder se = new SequenceEncoder(';');
      se.append(descInput.getValueString())
        .append(rotInput.getValueBoolean())
        .append(xInput.getIntValue(0))
        .append(yInput.getIntValue(0))
        .append(findInput.getValueString())
        .append(detachInput.getValueString());

      return ID + se.getValue();
    }

    @Override
    public String getState() {
      return "";
    }
  }
}
