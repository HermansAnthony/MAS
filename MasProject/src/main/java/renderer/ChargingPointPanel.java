package renderer;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.common.base.Optional;
import energy.Charger;
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

import java.util.List;

/**
 * A charging point panel that gives a live view of the current statistics of the charging point (including occupation etc).
 * @author Anthony Hermans, Federico Quin
 */
public final class ChargingPointPanel extends AbstractModelVoid
        implements PanelRenderer {

    private static final int PREFERRED_SIZE = 125;
    private final EnergyModel energyModel;
    private boolean chargersInitialized;
    Optional<Table> statsTable;
    Optional<Table> chargeStatsTable;

    /**
     * Create a new instance using the specified {@link ChargingPointPanel} which
     * supplies the statistics.
     * @param energyModel needed to represent the information.
     */
    public ChargingPointPanel(EnergyModel energyModel) {
        this.energyModel = energyModel;
        this.statsTable = Optional.absent();
        this.chargersInitialized = false;
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

        final String[] statsTitles = new String[] {"Drone", "Current occupation", "Reserved occupation"};
        for (int i = 0; i < statsTitles.length; i++) {
            final TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(statsTitles[i]);
            table.getColumn(i).pack();
        }

        final TableItem firstRow = new TableItem(table, 0);
        firstRow.setText("LW");
        final TableItem secondRow = new TableItem(table, 1);
        secondRow.setText("HW");

        final Table secondTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
            | SWT.V_SCROLL | SWT.H_SCROLL);
        chargeStatsTable = Optional.of(secondTable);
        secondTable.setHeaderVisible(true);
//        secondTable.setLinesVisible(true); TODO don't really visualize lines?
        final String[] titles = new String[] {"ChargerID", "Occupation"};
        for (int i = 0; i < titles.length; i++) {
            final TableColumn column = new TableColumn(secondTable, SWT.NONE);
            column.setText(titles[i]);
        }
        for (int i = 0; i < titles.length; i++) {
            secondTable.getColumn(i).pack();
        }
    }

    private void setOccupationStats(ChargingPoint chargingPoint){
        statsTable.get().getItem(0).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, false)*100)+'%');
        statsTable.get().getItem(0).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, true)*100)+'%');
        statsTable.get().getItem(1).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, false)*100)+'%');
        statsTable.get().getItem(1).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, true)*100)+'%');
    }

    private void setStats(List<Charger> chargers, int index){
        for (Charger entry : chargers){
            String droneID = "-";
            String chargeID = String.valueOf(index);
            if (entry.isDronePresent()){
                droneID = entry.getCurrentDrone().getDroneString();
            }
            chargeStatsTable.get().getItem(index).setText(0, chargeID);
            chargeStatsTable.get().getItem(index).setText(1, droneID);
            index++;
        }
    }
    private void setChargerStats(ChargingPoint chargingPoint){
        List<Charger> chargersLW =  chargingPoint.getChargeStations(DroneLW.class);
        List<Charger> chargersHW =  chargingPoint.getChargeStations(DroneHW.class);
        setStats(chargersLW, 0);
        setStats(chargersHW, chargersLW.size());
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
        // Init the rows of the chargers only once
        if (energyModel.getChargingPoint() != null && !chargersInitialized){
            for (int i = 0; i < 10; i++) {new TableItem(chargeStatsTable.get(), i);}
            this.chargersInitialized = true;
        }
        statsTable.get().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (statsTable.get().isDisposed()) {
                    return;
                }
                ChargingPoint chargingPoint = energyModel.getChargingPoint();
                if (chargingPoint == null){return;}
                setOccupationStats(chargingPoint);
                setChargerStats(chargingPoint);
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
