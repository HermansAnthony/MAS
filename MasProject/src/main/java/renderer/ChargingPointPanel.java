package renderer;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.common.base.Optional;
import energy.ChargingPoint;
import energy.EnergyModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import pdp.DroneHW;
import pdp.DroneLW;

/**
 * A charging point panel that gives a live view of the current statistics of the charging point (including occupation etc).
 * @author Anthony Hermans, Federico Quin
 */
public final class ChargingPointPanel extends AbstractModelVoid
        implements PanelRenderer {

    private static final int PREFERRED_SIZE = 225;
    private final EnergyModel energyModel;
    Optional<Table> statsTable;

    /**
     * Create a new instance using the specified {@link ChargingPointPanel} which
     * supplies the statistics.
     * @param energyModel needed to represent the information.
     */
    public ChargingPointPanel(EnergyModel energyModel) {
        this.energyModel = energyModel;
        this.statsTable = Optional.absent();
    }

    @Override
    public void initializePanel(Composite parent) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = 2;
        layout.marginHeight = 2;
        layout.type = SWT.VERTICAL;
        parent.setLayout(layout);

        final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
        statsTable = Optional.of(table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        final String[] statsTitles = new String[] {"ChargingPoint", "Current occupation", "Reserved occupation"};
        for (int i = 0; i < statsTitles.length; i++) {
            final TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(statsTitles[i]);
            table.getColumn(i).pack();
        }
        final TableItem firstRow = new TableItem(table, 0);
        firstRow.setText("Lightweight drone");
//        System.out.println(String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, false)));
//        firstRow.setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, true)));
        final TableItem secondRow = new TableItem(table, 1);
        secondRow.setText("Heavyweight drone");
//        secondRow.setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, false)));
//        secondRow.setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, true)));
//        final StatisticsDTO stats = statsTracker.getStatistics();
//        final Field[] fields = stats.getClass().getFields();
//        for (final Field f : fields) {
//            final TableItem ti = new TableItem(table, 0);
//            ti.setText(0, f.getName());
//            try {
//                ti.setText(1, f.get(stats).toString());
//            } catch (final IllegalArgumentException | IllegalAccessException e) {
//                ti.setText(1, e.getMessage());
//            }
//        }
//        for (int i = 0; i < statsTitles.length; i++) {
//            table.getColumn(i).pack();
//        }

//        statsTracker.getEventAPI().addListener(new Listener() {
//                                                   @Override
//                                                   public void handleEvent(Event e) {
//                                                       verify(e instanceof StatsEvent);
//                                                       final StatsEvent se = (StatsEvent) e;
//                                                       if (eventList.getDisplay().isDisposed()) {
//                                                           return;
//                                                       }
//                                                       eventList.getDisplay().asyncExec(new Runnable() {
//                                                           @Override
//                                                           public void run() {
//                                                               final TableItem ti = new TableItem(eventList, 0);
//                                                               ti.setText(0, Long.toString(se.getTime()));
//                                                               ti.setText(1, Long.toString(se.getTardiness()));
//                                                           }
//                                                       });
//                                                   }
//                                               }, StatsProvider.EventTypes.PICKUP_TARDINESS,
//                StatsProvider.EventTypes.DELIVERY_TARDINESS);
    }

    @Override
    public int preferredSize() {
        return PREFERRED_SIZE;
    }

    @Override
    public int getPreferredPosition() {
        return SWT.LEFT;
    }

    @Override
    public String getName() {
        return "Charging point";
    }

    @Override
    public void render() {
        if (statsTable.get().isDisposed()
                || statsTable.get().getDisplay().isDisposed()) {
            return;
        }
        statsTable.get().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (statsTable.get().isDisposed()) {
                    return;
                }
                ChargingPoint chargingPoint = energyModel.getChargingPoint();
                if (chargingPoint == null){return;}
                statsTable.get().getItem(0).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, false)*100)+'%');
                statsTable.get().getItem(0).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, true)*100)+'%');
                statsTable.get().getItem(1).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, false)*100)+'%');
                statsTable.get().getItem(1).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, true)*100)+'%');
            }
        });
    }

    /**
     * @return A new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for the {@link ChargingPointPanel}.
     * @author Anthony Hermans, Federico Quin
     */
    public static class Builder extends ModelBuilder.AbstractModelBuilder<ChargingPointPanel, Void> {

        private static final long serialVersionUID = 756808574424058913L;

        Builder() {
            setDependencies(EnergyModel.class);
        }

        @Override
        public ChargingPointPanel build(DependencyProvider dependencyProvider) {
            EnergyModel energyModel = dependencyProvider.get(EnergyModel.class);
            return new ChargingPointPanel(energyModel);
        }
    }
}
