__kernel void convolution(__global const float * fst, int fst_size,
                          __global const float * snd, int snd_size,
                          __global float * result)
{
    int id = get_global_id(0);
    if (id >= fst_size * fst_size)
    {
        return;
    }
    int row = id / fst_size;
    int column = id % fst_size;
    int hm = (snd_size - 1) / 2;

    float value = 0.0f;
    for (int i = -hm; i <= hm; ++i)
    {
        for (int j = -hm; j <= hm; ++j)
        {
            int new_i = row + i;
            int new_j = column + j;

            if (new_i >= 0 && new_i < fst_size && new_j >= 0 && new_j < fst_size)
            {
                value += fst[new_i * fst_size + new_j] * snd[(i + hm) * snd_size + (j + hm)];
            }
        }
    }

    result[row * fst_size + column] = value;
}
