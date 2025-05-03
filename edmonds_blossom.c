#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <time.h>

#define MAXV 5000000
#define MAXE 50000000

typedef struct Edge {
    int u, v;
} Edge;

Edge edges[MAXE];
int head[MAXV];
int nxt[MAXE];
int to[MAXE];
int match[MAXV], base[MAXV], p[MAXV];
bool used[MAXV], blossom[MAXV];
int q[MAXV], qs, qe;
int n, m;

void addEdge(int u, int v) {
    static int edgeCount = 0;
    to[edgeCount] = v;
    nxt[edgeCount] = head[u];
    head[u] = edgeCount++;
    if (edgeCount % 1000000 == 0) {
        printf("Added %d edges.\n", edgeCount);
        fflush(stdout);
    }
}

int lca(int a, int b) {
    static bool visited[MAXV];
    memset(visited, 0, sizeof(bool) * n);
    while (1) {
        a = base[a];
        visited[a] = true;
        if (match[a] == -1) break;
        a = p[match[a]];
    }
    while (1) {
        b = base[b];
        if (visited[b]) return b;
        if (match[b] == -1) break;
        b = p[match[b]];
    }
    return -1;
}

void markPath(int v, int b, int children) {
    while (base[v] != b) {
        blossom[base[v]] = blossom[base[match[v]]] = true;
        p[v] = children;
        children = match[v];
        v = p[match[v]];
    }
}

int findPath(int root) {
    memset(used, 0, sizeof(bool) * n);
    memset(p, -1, sizeof(int) * n);
    for (int i = 0; i < n; i++) base[i] = i;

    qs = qe = 0;
    q[qe++] = root;
    used[root] = true;
    int steps = 0;

    while (qs < qe) {
        int v = q[qs++];
        for (int e = head[v]; e != -1; e = nxt[e]) {
            int u = to[e];
            if (base[v] == base[u] || match[v] == u) continue;
            if (u == root || (match[u] != -1 && p[match[u]] != -1)) {
                int curbase = lca(v, u);
                memset(blossom, 0, sizeof(bool) * n);
                markPath(v, curbase, u);
                markPath(u, curbase, v);
                for (int i = 0; i < n; i++) {
                    if (blossom[base[i]]) {
                        base[i] = curbase;
                        if (!used[i]) {
                            used[i] = true;
                            q[qe++] = i;
                        }
                    }
                }
            } else if (p[u] == -1) {
                p[u] = v;
                if (match[u] == -1) return u;
                u = match[u];
                used[u] = true;
                q[qe++] = u;
            }
        }
        steps++;
        if (steps % 10000 == 0) {
            printf("BFS steps: %d\n", steps);
            fflush(stdout);
        }
    }
    return -1;
}

int maxMatching() {
    memset(match, -1, sizeof(int) * n);
    int res = 0;
    int processed = 0;
    for (int i = 0; i < n; i++) {
        if (match[i] == -1) {
            if (i % 10000 == 0) {
                printf("Processing vertex %d\n", i);
                fflush(stdout);
            }

            int v = findPath(i);
            if (v != -1) {
                res++;
                for (int u = v, w; u != -1; u = w) {
                    w = match[p[u]];
                    match[u] = p[u];
                    match[p[u]] = u;
                }
            }
            processed++;
            if (processed % 1000 == 0) {
                printf("Processed %d unmatched vertices so far. Matches found: %d\n", processed, res);
                fflush(stdout);
            }
        }
    }
    return res;
}

void loadGraph(const char* filename) {
    FILE* f = fopen(filename, "r");
    if (!f) {
        perror("Unable to open file");
        exit(1);
    }

    n = 0;
    m = 0;
    memset(head, -1, sizeof(head));

    int u, v;
    while (fscanf(f, "%d,%d", &u, &v) == 2) {
        if (u >= n) n = u + 1;
        if (v >= n) n = v + 1;
        addEdge(u, v);
        addEdge(v, u);
        m++;
    }
    fclose(f);
    printf("Finished loading graph: %d vertices, %d edges.\n", n, m);
}

void saveMatching(const char* filename) {
    FILE* f = fopen(filename, "w");
    if (!f) {
        perror("Unable to open output file");
        exit(1);
    }

    for (int i = 0; i < n; i++) {
        if (match[i] != -1 && i < match[i]) {
            fprintf(f, "%d,%d\n", i, match[i]);
        }
    }
    fclose(f);
}

int main(int argc, char* argv[]) {
    if (argc != 3) {
        printf("Usage: %s input.csv output.csv\n", argv[0]);
        return 1;
    }

    clock_t start_time = clock();

    loadGraph(argv[1]);
    printf("Graph Loaded.\n");

    int matches = maxMatching();
    printf("Optimal Matching with %d edges.\n", matches);

    saveMatching(argv[2]);
    printf("Matched set saved.\n");

    clock_t end_time = clock();
    double total_seconds = (double)(end_time - start_time) / CLOCKS_PER_SEC;

    int hours = (int) (total_seconds / 3600);
    int minutes = (int) ((total_seconds - hours * 3600) / 60);
    int seconds = (int) (total_seconds - hours * 3600 - minutes * 60);

    printf("Total runtime: %d hours, %d minutes, %d seconds\n", hours, minutes, seconds);

    return 0;
}