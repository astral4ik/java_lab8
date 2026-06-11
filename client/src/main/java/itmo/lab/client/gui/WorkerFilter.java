package itmo.lab.client.gui;

import itmo.lab.data.Position;
import itmo.lab.data.Status;
import itmo.lab.data.Worker;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Набор критериев фильтрации, применяемых к таблице и канвасу. */
public class WorkerFilter {

    public String name = "";
    public Integer idMin, idMax;
    public Double salaryMin, salaryMax;
    public Double coordXMin, coordXMax;
    public Double coordYMin, coordYMax;
    public LocalDate dateMin, dateMax;
    public Set<Position> positions = EnumSet.noneOf(Position.class);
    public Set<Status>   statuses  = EnumSet.noneOf(Status.class);
    public Set<String> organizations = new HashSet<>();
    public Set<String> owners = new HashSet<>();

    /** Возвращает {@code true}, если ни один критерий не задан. */
    public boolean isEmpty() {
        return name.isEmpty()
                && idMin == null && idMax == null
                && salaryMin == null && salaryMax == null
                && coordXMin == null && coordXMax == null
                && coordYMin == null && coordYMax == null
                && dateMin == null && dateMax == null
                && positions.isEmpty()
                && statuses.isEmpty()
                && organizations.isEmpty()
                && owners.isEmpty();
    }

    /** Возвращает {@code true}, если работник удовлетворяет всем активным критериям. */
    public boolean matches(Worker w) {
        if (!name.isEmpty() && (w.getName() == null
                || !w.getName().toLowerCase().contains(name.toLowerCase()))) return false;
        if (idMin != null && w.getId() < idMin) return false;
        if (idMax != null && w.getId() > idMax) return false;
        if (salaryMin != null && w.getSalary() < salaryMin) return false;
        if (salaryMax != null && w.getSalary() > salaryMax) return false;
        if (coordXMin != null && (w.getCoordinates() == null || w.getCoordinates().getX() < coordXMin)) return false;
        if (coordXMax != null && (w.getCoordinates() == null || w.getCoordinates().getX() > coordXMax)) return false;
        if (coordYMin != null && (w.getCoordinates() == null || w.getCoordinates().getY() < coordYMin)) return false;
        if (coordYMax != null && (w.getCoordinates() == null || w.getCoordinates().getY() > coordYMax)) return false;
        if (dateMin != null && (w.getStartDate() == null
                || w.getStartDate().toLocalDate().isBefore(dateMin))) return false;
        if (dateMax != null && (w.getStartDate() == null
                || w.getStartDate().toLocalDate().isAfter(dateMax))) return false;
        if (!positions.isEmpty() && !positions.contains(w.getPosition())) return false;
        if (!statuses.isEmpty() && !statuses.contains(w.getStatus())) return false;
        if (!organizations.isEmpty()) {
            String fn = w.getOrganization() != null ? w.getOrganization().getFullName() : null;
            if (!organizations.contains(fn)) return false;
        }
        if (!owners.isEmpty() && !owners.contains(w.getOwnerLogin())) return false;
        return true;
    }
}
