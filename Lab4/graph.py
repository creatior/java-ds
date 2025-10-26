import csv
import matplotlib.pyplot as plt
from collections import defaultdict

csv_file = "results_scatter.csv" 

data = defaultdict(list)

with open(csv_file, newline='') as f:
    reader = csv.reader(f)
    for row in reader:
        threads = int(row[0])
        size = int(row[1])
        time = float(row[2])
        data[threads].append((size, time))

plt.figure(figsize=(10, 6))

for threads, values in sorted(data.items()):
    values.sort(key=lambda x: x[0])
    sizes = [v[0] for v in values]
    times = [v[1] for v in values]
    plt.plot(sizes, times, marker='o', label=f"{threads} threads")

plt.xscale("log")
plt.xlabel("Размер задачи (N)")
plt.ylabel("Время выполнения (ms)")
plt.title("Зависимость времени выполнения от размера задачи")
plt.legend()
plt.grid(True, which="both", ls="--", lw=0.5)
plt.tight_layout()
plt.show()
