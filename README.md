# Large Scale Data Processing: Final Project
## Results (Harim Kim / Matching / Worked Alone)
### Solution Files
* www.harimkim.com

| Input File Name             | Output File Name                     | Matching Size |
| --------------------------- | ------------------------------------ | ------------- |
| log_normal_100.csv          | log_normal_100_solution.csv          |  |
| musae_ENGB_edges.csv        | musae_ENGB_edges_solution.csv        |  |
| soc-pokec-relationships.csv | soc-pokec-relationships_solution.csv |  |
| soc-LiveJournal1.csv        | soc-LiveJournal1_solution.csv        |  |
| twitter_original_edges.csv  | twitter_original_edges_solution.csv  |  |
| com-orkut.ungraph.csv       | com-orkut.ungraph_solution.csv       |  |

***
### Code
* Can be found in this repository

***
### Objectives
* Matching for Small Inputs (Edmonds' Blossom Algorithm)

For small inputs (log_normal_100.csv and musae_ENGB_edges.csv), it was presumed possibled to attempt an algorithm that can provide optimal matching, rather than relying on approximations. I referenced Edmonds' Blossom Algorithm and code from NetworkX and Pandas library on Python to compute the exact optimal matching for the two files, and obtained the results below.

| File Name            | Matching Size | Approach         | Runtime (Local) |
| -------------------- | ------------- | ---------------- | --------------- |
| log_normal_100.csv   | 50            | Edmonds' Blossom | 1s              |
| musae_ENGB_edges.csv | 2968          | Edmonds' Blossom | 1s              |

***
Matching for Large Inputs (Israeli-Itai Algorithm)

As an experimental attempt, Edmonds' Blossom Algorithm was run with soc-pokec-relation.csv file overnight, and it was found out that the program had fallen into extreme loops, taking 10,000-100,000 search iterations to process as it reached 10,000,000+ vertices. Due to this reason, the Israeli-Itai Algorithm that we learned from class was implemented in Spark for computability, scalability, and parallelization.

There were several challenges in implementation, and the first issue was breaking symmetries. When node u proposed to node v ((u,v)-proposal) and node w also proposed to node v ((w,v)-proposal), and it happened to be the case that both node u and node w were assigned 0 and node v was assigned a 1, it caused conflicts where both proposals were considered to be accepted and were added to the matched set. However, this caused the issue of the output file containing multiple edges that shared vertices, making the set an invalid match. To solve this problem, a simple symmetry-breaking adjustment was made, where the "candidateMatches" were assigned a random hash value, and was added to the matched set ("confirmedMatches") based on the priority of the hash value.

Another challenge that came with the mentioned symmetry-breaking issue was that both the local machine and GCP (due to limitations of student license) ran into Java Out of Memory Error. This was mainly due to two factors, first being the excessive .collect() and .persist() calls throughout the original program. Although excessive and unncessary .collect() were able to reduced, .persist() was still necessary for both the debugging purposes and the iterative nature of Isaeli-Itai Algorithm. Therefore, the second implementational adjustment made was utilizing .persist(StorageLevel.DISK_ONLY) to preserve the results from each iteration to reduce memory overload while maintaining the results of each iteration. Later, an output_merger.scala program was made to combine the results of the iterations and merge them into one file.

From the second adjustment, I realized that 1) although the theoretical runtime did not change, practical runtime is affected by writing the results into the hard drive, and 2) there are several iterations later in the algorithm that produce very few matches or even no matches. It was assumed that the 1/4 probability of proposal leading to a match made it difficult for multiple edges to be matched in a single iteration as the graph became sparse. To address both problems, at once, I modified the Israeli-Itai algorithm to perform a fallback to Greedy Random Matching algorithm when it was deemed that the graph has been brought down to a small size. To determine the mentioned "small-size", a zeroMatchStreak was introduced, a variable the was incremented by 1 every time no match was formed in a given iteration. This way, when the zeroMatchStreak reaches 3 (arbitrary selected value), the algorithm would determine the graph to be sparse enough for Greedy Random Matching algorithm, and form matches for the remaining edges in a deterministic way.

| File Name                   | Matching Size | Approach              | Runtime (Local) | Iteration (+ Greedy) | Runtime (GCP) |
| --------------------------- | ------------- | --------------------- | --------------- | -------------------- | ------------- |
| soc-pokec-relationships.csv | 599,709       | Israeli-Itai + Greedy |                 |                      |               |
| soc-LiveJournal1            | 1,578,566     | Israeli-Itai + Greedy | 16m 37m         | 42 + 2               |               |
| twitter_original_edges      | 92,404        | Israeli-Itai + Greedy | 20m 40s         | 27                   |               |
| com-orkut.ungraph.csv       | 1,339,741     | Israeli-Itai + Greedy | 38m 10s         | 42                   |               |

***
Finding Augmenting Paths

After 2-3 repeated trials on each of the larger samples to test for robustness and precision of the output, it was concluded that the Israeli-Itai Algorithm had been implemented corrected. To improve the results, an attempt to identify and flip the augmenting paths was implemented.

As searching for augmenting paths are non-trivial, the initial approach was to fully abandon parallelization and implement an algorithm that searchs every augmenting path of a given length (n), by providing a scenario in which n edges are augmenting paths. In other words, the program was to iterate through the nodes and find a condition in which 0-1 are not matched, 1-2 are matched, 2-3 are not matched, and so on. However, through Profession Su's suggestion, it was deemed better to simultaneously search for some of the augmenting paths instead of iteratively finding all of them, as the prior algorithm had significantly faster runtime than the latter, and the advantages of finding "all" augmenting paths of length n was not beneficial enough relative to the computing power that it required.

Therefore, an alternative algorithm (augmenting_path_improver.scala) was implemented, searching for augmenting paths of length 3 at parallel at each iteration, and resolving conflicts when they occur. For submission purposes, the augmenting path algorithm was run once on each large dataset once, then iterated 9 more times. For the final submission output files, the algorithm was run as much as time allowed.

| File Name                   | Original Matching | After 1 Iteration | After 10 Iterations | Runtime per Iteration |
| --------------------------- | ----------------- | ----------------- | ------------------- | --------------------- |
| soc-pokec-relationships.csv | 599,709           | 623,483           | 700,331             | 1-2 minutes           |
| soc-LiveJournal1            | 1,578,566         | 1,692,282         | 1,890,074           | 2-4 minutes           |
| twitter_original_edges      | 92,404            |                   |                     |                       |
| com-orkut.ungraph.csv       | 1,339,741         |                   |                     |                       |

***
Algorithm Analysis

| Algorithm Name         | Time-Complexity  | Space-Complexity | Scalability            | Parellelization |
| ---------------------- | ---------------- | ---------------- | ---------------------- | --------------- |
| Edmonds' Blossom       | O(V<sup>3</sup>) | O(V + E)         | No                     | No              |
| Israeli-Itai           | O(E) * k         | O(V + E)         | Yes                    | Yes             |
| Greedy Random Matching | O(E') * l        | O(V + E')        | Yes (when E' is small) | No              |
| Augmenting Path        | O(E + plog(p)    | O(m + n + p)     | Yes                    | Yes             |

Legend:
V = Number of Vertices
V' = Number of Vertices Left
E = Number of Edges
k = Number of Israeli-Itai Iterations
l = Number of Greedy Random Matching Iterations
p = Number of Augmenting Paths FOUND

Edmonds' Blossom Algorithm has been borrowed from a readily-distributed library and therefore its time and space complexity are fixed to the widely studied results.

The Israeli-Itai + Greedy Random Matching Algorithm has time-complexity of O(E * k) and O(E' * l). For Israeli-Itai, the algorithm is parallelizable as it utilizes RDD through Spark, while Greedy Random Matching is not. This is due to the how the Greedy Random Matching makes the decisions for forming a match, and it cannot be parallelized. However, both algorithms are deemed scalable, as the entire algorithm is expected to only fall back to Greedy Random Matching when E' is significantly smaller than E, allowing the computational complexity to be drastically reduced at the point of fallback. Regarding the space complexity, the initial loading of the edges take up O(m) space, and the broadcasts including activeVertices (remaining vertices), vertexBits (randomly assigned bits), and confirmedMatches (list of matches made in a given iteration) take up O(n) space. However, regarding the intermediate results and groupings of edges, they are programmed to be recorded directly at the hard drive, limiting the coefficient of O(m) from growing unmanageably large.

The algorithm to find augmenting path has time-complexity of O(E + plog(p)). This is because the comparison between the original edges and the matched set consumes O(m) time, and the shuffling process incorporated in resolving conflicts for overlapping augmenting paths requires O(plog(p) time. Though the algorithm mostly consists of map functions and are generally parallizable in the way it is programmed in Scala, there are two main bottlenecks that make this algorithm far from fully efficient.

First challenge is how the algorithm resolves conflicts among found augmenting paths through groupByKey, which is parallelizable and scalable, but cannot guarantee realistic runtime regardless of the input size. Secondly, rising from the first issue and the usage of collect() and broadcast(), the algorithm may not be considered fully scalable or cost-efficient considering its limitations in finding augmenting paths per iteration. In other words, although the algorithm is scalable, parallelizable, and relatively efficient, in graphs larger than com-orkut.ungraph.csv, it may be unreasonable to pay the amount of time and computing power thing algorithm requires to simply find "some" of the length 3 augmenting paths that exist in the current matched set.

***
Future Improvements & Studies

There are several additional procedures that I would have carried out if I had more time and access to stronger computing power. The first is that I am unsure of the exact benefits of the fallback to the Greedy Random Matching Algorithm, as it came from a theoretical standpoint of observing later Israeli-Itai iteration results that showed very few matches or even no matches. Unlike my expectations, the 3-5 trials that I was able to carry out on the larger data (as smaller input would rarely have meaningful number of iterations to begin with), did not show noticeable differences in the total number of iterations nor the runtime.

The second part that I would like to study and investigate further regards the augmenting paths. Due to the nature of the assignment, my primary goal in implementing and testing out the augmenting path algorithm was the boost the matching size of the solutions, and therefore, the approach the I took was to mainly boost soc-LiveJournal1.csv and com-orkut.ungraph.csv, as they had the largest room for improvements relative to percent change. Unfortunately, what I was unable to address due to this approach was what exact 1/ε-approximation I am reaching through the iterations of augmenting path algorithm, and if, at a certain point, it would be/would have been more beneficial to improve my original algorithm and implement one that searches for augmenting paths of length 5 or more. If I had almost run out of augmenting paths of length 3 after a certain number of iterations, for a large and relatively well distributed data such as soc-pokec-relationships.csv, or soc-LiveJournal1.csv it could have been better to implement such an algorithm for a larger increase in the size of the matched set. 

***
Procedure of Algorithm

To replicate the results, each algorithm can be run in the following steps.

File Structure (Empty directories should still be in the repository)
Root
|-data (directory)
|--all input.csv
|--output (directory)

Small Input (log_normal_100.csv, musae_ENGB_edges.csv)



Large Input (other csv files)


***

For the final project, you are provided 6 CSV files, each containing an undirected graph, which can be found [here](https://drive.google.com/file/d/1khb-PXodUl82htpyWLMGGNrx-IzC55w8/view?usp=sharing). The files are as follows:  

|           File name           |        Number of edges       |
| ------------------------------| ---------------------------- |
| com-orkut.ungraph.csv         | 117185083                    |
| twitter_original_edges.csv    | 63555749                     |
| soc-LiveJournal1.csv          | 42851237                     |
| soc-pokec-relationships.csv   | 22301964                     |
| musae_ENGB_edges.csv          | 35324                        |
| log_normal_100.csv            | 2671                         |  


You can choose to work on **matching** or **correlation clustering**. 

## Matching

Your goal is to compute a matching as large as possible for each graph. 

### Input format
Each input file consists of multiple lines, where each line contains 2 numbers that denote an undirected edge. For example, the input below is a graph with 3 edges.  
1,2  
3,2  
3,4  

### Output format
Your output should be a CSV file listing all of the matched edges, 1 on each line. For example, the ouput below is a 2-edge matching of the above input graph. Note that `3,4` and `4,3` are the same since the graph is undirected.  
1,2  
4,3  

## Correlation Clustering

Your goal is to compute a clustering that has disagreements as small as possible for each graph. 

### Input format
Each input file consists of multiple lines, where each line contains 2 numbers that denote an undirected edge. For example, the input below is a graph with 3 (positive) edges.  
1,2  
3,2  
3,4  

The 3 remaining pairs of vertices that do not appear in the above list denote negative edges. They are (2,4), (1,4), (1,3).

### Output format
Your output should be a CSV file describing all of the clusters. The number of lines should be equal to the number of vertices. Each line consists two numbers, the vertex ID and the cluster ID.

For example, the output below denotes vertex 1, vertex 3, and vertex 4 are in one cluster and vertex 2 forms a singleton cluster.  The clustering has a 4 disagreements.  
1,100  
2,200  
4,100  
3,100  


## No template is provided
For the final project, you will need to write everything from scratch. Feel free to consult previous projects for ideas on structuring your code. That being said, you are provided a verifier that can confirm whether or not your output is a matching or a clustering. As usual, you'll need to compile it with
```
sbt clean package
```  
### Matching

The matching verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.matching_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_matching.csv

// Unix
spark-submit --master "local[*]" --class "final_project.matching_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_matching.csv
```

### Correlation Clustering

The clustering verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file describing the clustering. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.clustering_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_clustering.csv

// Unix
spark-submit --master "local[*]" --class "final_project.clustering_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_clustering.csv

```

## Deliverables
* The output file for each test case.
  * For naming conventions, if the input file is `XXX.csv`, please name the output file `XXX_solution.csv`.
  * You'll need to compress the output files into a single ZIP or TAR file before pushing to GitHub. If they're still too large, you can upload the files to Google Drive and include the sharing link in your report.
* The code you've applied to produce the solutions.
  * You should add your source code to the same directory as the verifiers and push it to your repository.
* A project report that includes the following:
  * A table containing the objective of the solution (i.e. the size of matching or the number of disagreements of clustering) you obtained for each test case. The objectives must correspond to the matchings or the clusterings in your output files.
  * An estimate of the amount of computation used for each test case. For example, "the program runs for 15 minutes on a 2x4 N1 core CPU in GCP." If you happen to be executing mulitple algorithms on a test case, report the total running time.
  * Description(s) of your approach(es) for obtaining the matching or the clustering. It is possible to use different approaches for different cases. Please describe each of them as well as your general strategy if you were to receive a new test case. It is important that your approach can scale to larger cases if there are more machines.
  * Discussion about the advantages of your algorithm(s). For example, does it guarantee a constraint on the number of shuffling rounds (say `O(log log n)` rounds)? Does it give you an approximation guarantee on the quality of the solution? If your algorithm has such a guarantee, please provide proofs or scholarly references as to why they hold in your report.
* A 10-minute presentation during class time on 4/29 (Tue) and 5/1 (Thu).
  * Note that the presentation date is before the final project submission deadline. This means that you could still be working on the project when you present. You may present the approaches you're currently trying. You can also present a preliminary result, like the matchings or the clusterings you have at the moment.

## Grading policy
* Quality of solutions (40%)
  * For each test case, you'll receive at least 70% of full credit if your matching size is at 70% the best answer in the class or if your clustering size is at most 130% of the best in the class.
  * **You will receive a 0 for any case where the verifier does not confirm that your output is a correct.** Please do not upload any output files that do not pass the verifier.
* Project report (35%)
  * Your report grade will be evaluated using the following criteria:
    * Discussion of the merits of your algorithms such as the theoretical merits (i.e. if you can show your algorithm has certain guarantee).
    * The scalability of your approach
    * Depth of technicality
    * Novelty
    * Completeness
    * Readability
* Presentation (15%)
* Formatting (10%)
  * If the format of your submission does not adhere to the instructions (e.g. output file naming conventions), points will be deducted in this category.

## Submission via GitHub
Delete your project's current **README.md** file (the one you're reading right now) and include your report as a new **README.md** file in the project root directory. Have no fear—the README with the project description is always available for reading in the template repository you created your repository from. For more information on READMEs, feel free to visit [this page](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/about-readmes) in the GitHub Docs. You'll be writing in [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown). Be sure that your repository is up to date and you have pushed all of your project's code. When you're ready to submit, simply provide the link to your repository in the Canvas assignment's submission.

## You must do the following to receive full credit:
1. Create your report in the ``README.md`` and push it to your repo.
2. In the report, you must include your teammates' full name in addition to any collaborators.
3. Submit a link to your repo in the Canvas assignment.

## Deadline and early submission bonus
1. The deadline of the final project is on 5/4 (Sunday) 11:59PM.  
2. **If you submit by 5/2 (Friday) 11:59PM, you will get 5% boost on the final project grade.**  
3. The submission time is calculated from the last commit in the Git log.  
4. **No extension beyond 5/4 11:59PM will be granted, even if you have unused late days.**  
