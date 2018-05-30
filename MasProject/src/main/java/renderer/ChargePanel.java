package renderer;

import com.google.common.base.Optional;
import energy.ChargingPoint;
import energy.EnergyModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import pdp.Drone;
import pdp.DroneHW;
import pdp.DroneLW;
import util.Tuple;

import java.util.List;

public class ChargePanel {
    private final EnergyModel energyModel;
    private boolean chargersInitialized;
    Optional<Table> statsTable;
    Optional<Table> chargeStatsTable;
    private Shell shell;

    public ChargePanel(Display display, EnergyModel energyModel){
        this.energyModel = energyModel;
        this.chargersInitialized = false;
        this.statsTable = Optional.absent();
        this.chargeStatsTable = Optional.absent();
        this.shell = new Shell(display.getActiveShell());
        this.shell.setText("Charging point statistics");
        final FillLayout layout = new FillLayout();
        layout.marginWidth = 500;
        layout.marginHeight = 175;
        layout.type = SWT.VERTICAL;
        this.shell.setLayout(layout);
    }

    private void setOccupationStats(ChargingPoint chargingPoint){
        statsTable.get().getItem(0).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, false)*100)+'%');
        statsTable.get().getItem(0).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneLW.class, true)*100)+'%');
        statsTable.get().getItem(1).setText(1, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, false)*100)+'%');
        statsTable.get().getItem(1).setText(2, String.valueOf(chargingPoint.getOccupationPercentage(DroneHW.class, true)*100)+'%');
    }

    private void setStats(List<Tuple<Drone, Boolean>> chargers, int index){
        for (Tuple<Drone, Boolean> entry : chargers){
            String droneID = "-";
            String chargeID = String.valueOf(index);
            if (entry != null){
                if (entry.second) droneID = entry.first.getDroneString();
            }
            chargeStatsTable.get().getItem(index).setText(0, chargeID);
            chargeStatsTable.get().getItem(index).setText(1, droneID);
            index++;
        }
    }

    private void setChargerStats(ChargingPoint chargingPoint){
        List<Tuple<Drone, Boolean>> chargersLW =  chargingPoint.getChargeStations(DroneLW.class);
        List<Tuple<Drone, Boolean>> chargersHW =  chargingPoint.getChargeStations(DroneHW.class);
        setStats(chargersLW, 0);
        setStats(chargersHW, chargersLW.size());
    }

    public void initializePanel() {
        shell.addListener(SWT.Close, d -> shell.dispose());
        final Table table = new Table(this.shell,SWT.MULTI | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
        statsTable = Optional.of(table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        final String[] statsTitles = new String[] {"Drone", "Current occupation", "Reserved occupation"};
        for (int i = 0; i < statsTitles.length; i++) {
            final TableColumn column = new TableColumn(table, SWT.FILL);
            column.setText(statsTitles[i]);
        }

        final TableItem firstRow = new TableItem(table, 0);
        firstRow.setText(0,"LW");
        firstRow.setText(1 ,"0");
        firstRow.setText(2 ,"0");
        final TableItem secondRow = new TableItem(table, 1);
        secondRow.setText(0,"HW");
        secondRow.setText(1 ,"0");
        secondRow.setText(2 ,"0");
        for (int i = 0; i < statsTitles.length; i++) {
            table.getColumn(i).pack();
        }
        table.setSize(400,150);

        final Table secondTable = new Table(this.shell, SWT.MULTI | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
        chargeStatsTable = Optional.of(secondTable);
        secondTable.setHeaderVisible(true);
        secondTable.setLinesVisible(true);
        final String[] titles = new String[] {"ChargerID", "Occupation"};
        for (int i = 0; i < titles.length; i++) {
            final TableColumn column = new TableColumn(secondTable, SWT.NONE);
            column.setText(titles[i]);
        }
        shell.open();
    }

    public void render() {
        if (statsTable.get().isDisposed()
                || statsTable.get().getDisplay().isDisposed()
                || chargeStatsTable.get().isDisposed()
                || chargeStatsTable.get().isDisposed()
                || shell.getDisplay().isDisposed()
                || shell.isDisposed()) {
            return;
        }
        // Init the rows of the chargers only once
        if (energyModel.getChargingPoint() != null && !chargersInitialized){
            for (int i = 0; i < 10; i++) {
                final TableItem row = new TableItem(chargeStatsTable.get(), i);
                row.setText(0,"-");
                row.setText(1,"-");
            }
            for (int i = 0; i < 2; i++) {
                chargeStatsTable.get().getColumn(i).pack();
            }
            //chargeStatsTable.get().setSize(chargeStatsTable.get().computeSize(200,180));
            this.chargersInitialized = true;
        }
        Runnable displayTask = () -> displayStats();
        this.shell.getDisplay().asyncExec(displayTask);
    }
    private void displayStats(){
        try {
            if (statsTable.get().isDisposed() || chargeStatsTable.get().isDisposed() || this.shell.isDisposed()) {
                return;
            }
            ChargingPoint chargingPoint = energyModel.getChargingPoint();
            if (chargingPoint == null) {
                return;
            }
            setOccupationStats(chargingPoint);
            setChargerStats(chargingPoint);
            render();
        } catch (Exception e){
            return;
        }
    }
}
