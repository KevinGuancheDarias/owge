interface Sortable {id: number; order: number}

export class OrderUtil {
    private constructor() {
        // An util ...
    }

    public static doOrder<T extends Sortable>(items: T[]): T[] {
        return items.sort((a,b) => this.compareForSort(a,b));
    }

    public static compareForSort(a: Sortable, b: Sortable): number {
        return this.getIdOrderOrId(a) > this.getIdOrderOrId(b) ? 1 : -1;
    }

    private static getIdOrderOrId(sortable: Sortable): number {
        return sortable.order ?? sortable.id;
    }
}
